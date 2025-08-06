package searchengine.services.indexing;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Messages;
import searchengine.config.Site;
import searchengine.config.SiteList;
import searchengine.dto.Response;
import searchengine.dto.indexing.IndexingErrorResponse;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.utills.parsing.sitemapping.SiteParser;
import searchengine.utills.parsing.sitemapping.Utils;

import javax.transaction.Transactional;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;


@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private ThreadPoolExecutor executor;

    private final SiteParser siteParser;
    private final SiteList siteListFromConfig;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final List<SiteEntity> siteEntityList = new ArrayList<>();

    @Override
    @Transactional
    public Response startIndexing() {
        Response response;
        SiteParser.setCancel(false);
        if (isIndexingSuccessful()) {
            IndexingResponse responseTrue = new IndexingResponse();
            responseTrue.setResult(true);
            response = responseTrue;
        } else {
            IndexingErrorResponse responseFalse = new IndexingErrorResponse();
            responseFalse.setResult(false);
            responseFalse.setError(Messages.INDEXING_HAS_ALREADY_STARTED);
            response = responseFalse;
        }
        return response;
    }

    @Transactional
    public boolean isIndexingSuccessful() {
        if (siteListFromConfig.getSites().stream()
                .map(e -> siteRepository.countByNameAndStatus(e.getName(), Status.INDEXING))
                .reduce(0, Integer::sum) > 0) {
            return false;
        }
        siteParser.clearUniqueLinks();

        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("Site: %d")
                .build();
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1, threadFactory);
        executor.setMaximumPoolSize(Runtime.getRuntime().availableProcessors());

        siteListFromConfig.getSites().forEach(e -> {
            boolean isCreate = !siteRepository.existsByName(e.getName());
            if (SiteParser.isCancel()) {
                executor.shutdownNow();
            } else {
                executor.execute(() -> parseOneSite(e.getUrl(), e.getName(), isCreate));
            }
        });

        executor.shutdown();
        return true;
    }

    @Transactional
    public void parseOneSite(String url, String name, boolean isCreate) {
        if (SiteParser.isCancel()) {
            return;
        }
        SiteEntity siteEntity;
        int siteId;
        if (isCreate) {
            siteEntity = new SiteEntity(Status.INDEXING, Utils.setNow(), url, name);
            log.info("<<<=== Site '{}' added", name);
        } else {
            siteEntity = siteRepository.findByName(name).orElse(null);
            if (siteEntity == null) {
                log.warn("Сайт {} не найден", name);
                return;
            }
            siteEntity.setStatus(Status.INDEXING);
            log.info("<<<=== Site '{}' changed", siteEntity.getName());
            deleteByName(name);
        }

        siteEntity = siteRepository.save(siteEntity);
        siteId = siteEntity.getSiteId();
        siteEntityList.add(siteEntity);

        try {
            String domain = Utils.getProtocolAndDomain(url);
            siteParser.initSiteParser(siteId, domain, url);
            siteParser.getLinks();
        } catch (IllegalArgumentException e) {
            log.error("Ошибка получения протокола и домена для URL '{}': {}", url, e.getMessage());
        }
    }

    void deleteByName(String name) {
        Optional<SiteEntity> siteByName = siteRepository.findByName(name);
        if (siteByName.isPresent()) {
            int siteId = siteByName.get().getSiteId();
            log.warn("lemma deleteAllBySiteId: {}", siteId);
            try {
                lemmaRepository.deleteAllBySiteId(siteId);
            } catch (Exception e) {
                log.error("lemmaRepository.deleteAllBySiteIdInBatch() message: {}", e.getMessage());
            }
            log.warn("page deleteAllBySiteId: {}", siteId);
            try {
                pageRepository.deleteAllBySiteId(siteId);
            } catch (Exception e) {
                log.error("pageRepository.deleteAllBySiteIdInBatch() message: {}", e.getMessage());
            }
        }
    }

    @Override
    public Response stopIndexing() {
        Response response;
        if (isStopIndexing()) {
            IndexingResponse responseTrue = new IndexingResponse();
            responseTrue.setResult(true);
            response = responseTrue;
        } else {
            IndexingErrorResponse responseFalse = new IndexingErrorResponse();
            responseFalse.setResult(false);
            responseFalse.setError(Messages.INDEXING_IS_NOT_RUNNING);
            response = responseFalse;
        }
        return response;
    }

    private boolean isStopIndexing() {
        try {
            long size = siteEntityList.stream().filter(e -> e.getStatus() == Status.INDEXING).count();
            if (size == 0) {
                log.warn(Messages.INDEXING_IS_NOT_RUNNING);
                return false;
            }

            siteParser.forceStop();

            siteEntityList.stream()
                    .filter(e -> e.getStatus() == Status.INDEXING)
                    .forEach(e -> {
                        e.setStatus(Status.FAILED);
                        e.setStatusTime(new Timestamp(System.currentTimeMillis()));
                        e.setLastError(Messages.INDEXING_STOPPED_BY_USER);
                    });
            siteRepository.saveAll(siteEntityList);
            log.warn(Messages.INDEXING_STOPPED_BY_USER);
        } catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public Response indexPage(String url) {
        Response response;
        if (isIndexPage(url)) {
            IndexingResponse responseTrue = new IndexingResponse();
            responseTrue.setResult(true);
            response = responseTrue;
        } else {
            IndexingErrorResponse responseFalse = new IndexingErrorResponse();
            responseFalse.setResult(false);
            responseFalse.setError(Messages.THIS_PAGE_IS_LOCATED_OUTSIDE_THE_SITES_SPECIFIED_IN_THE_CONFIGURATION_FILE);
            response = responseFalse;
        }
        return response;
    }

    private boolean isIndexPage(String url) {
        SiteParser.setCancel(false);
        String domain = Utils.getProtocolAndDomain(url);

        if (siteListFromConfig.getSites().stream().noneMatch(site -> site.getUrl().equals(domain))) {
            return false;
        }

        Site site = siteListFromConfig.getSites().stream()
                .filter(s -> s.getUrl().equals(domain))
                .findFirst()
                .orElse(null);

        if (site == null) {
            return false;
        }

        String name = site.getName();
        SiteEntity siteEntity = siteRepository.findByName(name).orElse(null);
        if (siteEntity == null) {
            siteEntity = new SiteEntity(Status.INDEXING, Utils.setNow(), domain, name);
        } else {
            siteEntity.setStatus(Status.INDEXING);
            siteEntity.setStatusTime(Utils.setNow());
            String path = url.substring(domain.length());
            deletePage(siteEntity.getSiteId(), path);
        }
        siteEntity.setLastError("");
        siteRepository.save(siteEntity);

        return saveLemmasAndIndicesForOnePage(url, siteEntity, domain);
    }

    private boolean saveLemmasAndIndicesForOnePage(String url, SiteEntity siteEntity, String domain) {
        Page page = null;
        try {
            page = siteParser.savePage(url, siteEntity, domain);
        } catch (Exception e) {
            log.warn("siteParser.savePage - error: {}", e.getMessage());
        }

        if (page == null) {
            return false;
        }
        siteParser.parseSinglePage(page);
        return true;
    }

    private void deletePage(int siteId, String path) {
        log.info("The page {} by sideId: {} is deleted", path, siteId);
        Page page = pageRepository.findBySiteIdAndPath(siteId, path);
        if (page != null) {
            deleteLemmas(page, siteId);
            pageRepository.delete(page);
        }
    }

    private void deleteLemmas(Page page, int siteId) {
        List<IndexEntity> indexList = indexRepository.findByPageId(page.getPageId());
        List<Lemma> lemmaList = new ArrayList<>();

        indexList.forEach(e -> {
            Lemma lemma = lemmaRepository.findByLemmaId(e.getLemmaId());
            if (lemma != null) {
                lemma.setFrequency(lemma.getFrequency() - 1);
                lemmaList.add(lemma);
            }
        });

        lemmaRepository.saveAll(lemmaList);
        log.info("Lemmas by pageId: {} are removed", page.getPageId());
        lemmaRepository.deleteBySiteIdAndFrequency(siteId, 0);
    }
}
