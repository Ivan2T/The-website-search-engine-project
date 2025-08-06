package searchengine.utills.parsing.sitemapping;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import searchengine.config.Messages;
import searchengine.model.Page;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.utills.parsing.ParseLemma;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Getter
@Setter
@Component
@RequiredArgsConstructor
public class SiteParser {

    private int siteId;
    private String url;
    private String domain;

    private final ParseLemma parseLemma;
    private ParsePageTask parsePageTask;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;

    private static AtomicBoolean isCancel = new AtomicBoolean(false);

    public static void setCancel(boolean b) {
        isCancel.set(b);
    }

    public static boolean isCancel() {
        return isCancel.get();
    }

    public void forceStop() {
        setCancel(true);
        pool.shutdownNow();
    }

    private static final int PARALLELISM = 120;
    private ForkJoinPool pool = new ForkJoinPool(PARALLELISM);

    public void getLinks() {
        long millisSecond = 2L;
        pool = new ForkJoinPool(PARALLELISM);
        parsePageTask = preparePage();
        pool.execute(parsePageTask);

        while (!parsePageTask.isDone() && !isCancel()) {
            try {
                TimeUnit.MILLISECONDS.sleep(millisSecond);
            } catch (InterruptedException ignored) {
                log.error("Page parsing error");
            }
        }

        if (isCancel()) {
            pool.shutdownNow();
            forceStop();
            log.info("Отмена индексации... ");
        } else {
            pool.shutdown();
        }
        try {
            parsePageTask.join();
            saveSite();
        } catch (Exception e) {
            log.error("parsePage.join() {}", e.getMessage());
        }
        parsePageTask = null;
    }

    private ParsePageTask preparePage() {
        parsePageTask = new ParsePageTask(parseLemma, pageRepository);
        parsePageTask.setUrl(url);
        parsePageTask.setDomain(domain);
        parsePageTask.setParent(null);
        parsePageTask.setSiteId(siteId);
        return parsePageTask;
    }

    private void saveSite() {
        SiteEntity siteEntity = siteRepository.findById(siteId).orElse(null);
        if (siteEntity == null) {
            log.warn("Сайт с ID: {} не найден", siteId);
            return;
        }
        siteEntity.setStatus(isCancel() ? Status.FAILED : Status.INDEXING);
        siteEntity.setStatusTime(Utils.setNow());

        getLemmasForAllPages(siteEntity);

        siteEntity.setStatus(isCancel() ? Status.FAILED : Status.INDEXED);
        siteEntity.setLastError(isCancel() ? Messages.INDEXING_STOPPED_BY_USER : "");
        siteEntity.setStatusTime(Utils.setNow());
        siteRepository.save(siteEntity);
        log.info("===>>> site '{}' saved", siteEntity.getName());
    }

    public void getLemmasForAllPages(SiteEntity siteEntity) {
        int statusCode = 200;
        List<Page> pageList = pageRepository.findBySiteIdAndCode(siteEntity.getSiteId(), statusCode);

        if (pageList.isEmpty()) {
            log.warn("No pages found for site {}", siteEntity.getName());
            return; // Прекращаем выполнение, если страниц нет
        }

        parseLemma.setBeginPos(pageList.get(0).getPageId());
        parseLemma.setEndPos(pageList.get(pageList.size() - 1).getPageId());

        pageList.stream().takeWhile(e -> !isCancel()).forEach(this::parseSinglePage);
    }


    public void parseSinglePage(Page page) {
        parseLemma.setCurrentPos(page.getPageId());
        if (!isCancel()) {
            parseLemma.parsing(page);
        }
    }

    public void clearUniqueLinks() {
        ParsePageTask.clearUniqueLinks();
    }

    public Page savePage(String url, SiteEntity siteEntity, String domain) {
        int statusCode = 200;
        preparePage();
        Document doc = parsePageTask.getDocumentByUrl(url);
        parsePageTask.setSiteId(siteEntity.getSiteId());
        parsePageTask.setDomain(domain);
        parsePageTask.setUrl(url);
        return parsePageTask.savePage(doc, statusCode);
    }

    public void initSiteParser(int siteId, String domain, String url) {
        this.siteId = siteId;
        this.domain = domain;
        this.url = url;
    }
}
