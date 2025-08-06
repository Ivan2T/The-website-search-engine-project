package searchengine.services.statistics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SiteList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SiteList sites;

    private final SiteRepository siteRepository;

    private final PageRepository pageRepository;

    private final LemmaRepository lemmaTRepository;

    @Override
    public StatisticsResponse getStatistics() {
        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        TotalStatistics total = new TotalStatistics();

        total.setSites(sites.getSites().size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();
        for (Site site : sitesList) {
            SiteEntity siteEntity = siteRepository.findByName(site.getName()).orElse(null);
            if (siteEntity == null) {
                continue;
            }

            int pagesCount = getPageCount(siteEntity);
            int lemmasCount = getLemmasCount(siteEntity);

            detailed.add(setDetailedStatisticsItem(site, siteEntity, pagesCount, lemmasCount));

            total.setPages(total.getPages() + pagesCount);
            total.setLemmas(total.getLemmas() + lemmasCount);
        }
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }

    private DetailedStatisticsItem setDetailedStatisticsItem(Site site, SiteEntity siteEntity, int pagesCount,
                                                             int lemmasCount) {
        DetailedStatisticsItem item = new DetailedStatisticsItem();
        item.setName(site.getName());
        item.setUrl(site.getUrl());
        item.setPages(pagesCount);
        item.setLemmas(lemmasCount);
        item.setStatus(siteEntity.getStatus().toString());

        item.setError(siteEntity.getLastError() == null && !siteEntity.getStatus().equals(Status.FAILED) ? ""
                : siteEntity.getLastError());

        item.setStatusTime(siteEntity.getStatusTime().getTime());
        return item;
    }

    private int getLemmasCount(SiteEntity siteEntity) {
        int lemmasCount = 0;
        try {
            lemmasCount = lemmaTRepository.countBySiteId(siteEntity.getSiteId());
        } catch (Exception e) {
            log.warn("lemmasCount = 0", e);
        }
        return lemmasCount;
    }

    private int getPageCount(SiteEntity siteEntity) {
        int pagesCount = 0;
        try {
            pagesCount = pageRepository.countBySiteId(siteEntity.getSiteId());
        } catch (Exception e) {
            log.warn("pagesCount = 0", e);
        }
        return pagesCount;
    }
}