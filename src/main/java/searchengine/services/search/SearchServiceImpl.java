package searchengine.services.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SiteList;
import searchengine.dto.Response;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SearchResults;
import searchengine.model.SiteEntity;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.utills.lemma.LemmaFinder;

import java.util.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private int countSearch;

    private final SiteList sites;

    private final SiteRepository siteRepository;

    private final PageRepository pageRepository;

    private final LemmaRepository lemmaRepository;

    private final SnippetFormatter snippetFormatter;

    private final RelevanceCalculator relevanceCalculator;

    private final LemmaFinder lemmaFinder = LemmaFinder.getInstance();

    @Override
    public Response search(String query, String site, int offset, int limit) {

        List<SearchResults> searchResultsList = new ArrayList<>();
        printInfoBySearch(query, site, offset, limit);

        List<Integer> siteIdList = new ArrayList<>();
        List<String> lemmaListFromQuery = new ArrayList<>();
        List<Lemma> lemmaList = new ArrayList<>();
        Response response = prepareDataForSearch(query, site, siteIdList, lemmaListFromQuery,
                lemmaList);

        if (response != null) {
            return response;
        }


        fillSearchResultsList(siteIdList, lemmaList, offset, limit, searchResultsList);

        if (searchResultsList.isEmpty()) {
            return setResponseFalse("Не найдено");
        }
        sortSearchResultsList(offset, limit, searchResultsList);

        setSnippetForSearchResults(lemmaList, searchResultsList);

        return setSearchData(searchResultsList);
    }

    private void printInfoBySearch(String query, String site, int offset, int limit) {
        System.err.println();
        log.info("Поисковый запрос: {}", query);
        log.info("Сайт: {}", site);
        log.info("Сдвиг от 0: {}", offset);
        log.info("Количество результатов: {}", limit);

    }

    private Response prepareDataForSearch(
            String query, String site,
            List<Integer> siteIdList, List<String> lemmaListFromQuery,
            List<Lemma> lemmaList) {


        List<Integer> siteIdListTemp = getSiteIdList(site);

        countSearch = 0;
        for (Integer siteId : siteIdListTemp) {
            countSearch += lemmaRepository.countBySiteId(siteId);
        }
        if (siteIdListTemp.isEmpty()) {
            return setResponseFalse("Search site " + site + " not found");
        }

        List<String> lemmaListFromQueryTemp = Objects.requireNonNull(lemmaFinder)
                .collectLemmas(query)
                .keySet()
                .stream()
                .toList();

        List<Lemma> lemmaListTemp = getLemmaList(siteIdListTemp, lemmaListFromQueryTemp);
        if (lemmaListTemp.isEmpty()) {
            return setResponseFalse("search lemmas: not found in database");
        }

        removeIfLimitFrequencyIsBig(lemmaListTemp);
        if (lemmaListTemp.isEmpty()) {
            return setResponseFalse("Not found lemmas in DB");
        }

        siteIdListTemp = lemmaListTemp.stream()
                .map(Lemma::getSiteId)
                .distinct()
                .toList();

        lemmaListTemp = lemmaListTemp.stream()
                .sorted(Comparator.comparingInt(Lemma::getFrequency))
                .toList();

        siteIdList.addAll(siteIdListTemp);
        lemmaListFromQuery.addAll(lemmaListFromQueryTemp);
        lemmaList.addAll(lemmaListTemp);

        return null;
    }

    private List<Integer> getSiteIdList(String site) {
        List<Integer> siteIdList = new ArrayList<>();
        if (site == null) {
            List<Site> siteList = sites.getSites();
            siteIdList = siteList.stream()
                    .map(Site::getName)
                    .map(s -> {
                        if (siteRepository.existsByName(s)) {
                            return siteRepository.findSiteEByName(s).get(0).getSiteId();
                        } else {
                            return 0;
                        }
                    }).toList();
        } else {
            Optional<Site> siteFromConfig = sites.getSites().stream()
                    .filter(site1 -> site1.getUrl().equals(site)).findFirst();
            if (siteFromConfig.isPresent()) {
                SiteEntity siteEntity = siteRepository.findByName(siteFromConfig.get().getName())
                        .orElse(null);
                if (siteEntity != null) {
                    siteIdList.add(siteEntity.getSiteId());
                }
            }
        }
        return siteIdList.stream()
                .filter(integer -> integer != 0)
                .toList();
    }

    private List<Lemma> getLemmaList(List<Integer> siteIdList, List<String> lemmaListFromQuery) {
        List<Lemma> lemmaList = new ArrayList<>();
        for (Integer siteId : siteIdList) {
            for (String lem : lemmaListFromQuery) {
                lemmaRepository.findBySiteIdAndLemma(siteId, lem).ifPresent(lemmaList::add);
            }
            long countOfWordsFound = lemmaList.stream()
                    .filter(lemma -> siteId.equals(lemma.getSiteId())).count();
            if (countOfWordsFound == 0) {
                continue;
            }
            if (countOfWordsFound != lemmaListFromQuery.size()) {
                lemmaList.removeIf(lemma -> lemma.getSiteId() == siteId);
            }
        }
        return lemmaList;
    }

    private void removeIfLimitFrequencyIsBig(List<Lemma> lemmaList) {
        int limitCount = 1000;
        Iterator<Lemma> iterator = lemmaList.iterator();
        while (iterator.hasNext()) {
            Lemma lemma = iterator.next();
            int countPages = pageRepository.countBySiteId(lemma.getSiteId());

            log.debug("siteId: {} countPages: {} Frequency: {}", lemma.getSiteId(), countPages,
                    lemma.getFrequency());
            if (lemma.getFrequency() >= countPages && countPages > limitCount) {
                log.warn("remove lemma:{}", lemma.getLemma());
                iterator.remove();
            }
        }
    }

    private void fillSearchResultsList(List<Integer> siteIdList,
                                       List<Lemma> lemmaList, int offset, int limit, List<SearchResults> searchResultsList) {
        double[][] relevance;

        List<SearchResults> searchResultsListTemp = new ArrayList<>();
        for (Integer i : siteIdList) {
            relevance = relevanceCalculator.formationForOneSite(
                    lemmaList.stream()
                            .filter(lemma -> lemma
                                    .getSiteId() == i)
                            .toList(), offset, limit,
                    searchResultsListTemp);

            for (int j = 0; j < relevance.length; j++) {
                SearchResults results;
                for (SearchResults searchResults : searchResultsListTemp) {
                    results = searchResults;
                    int ind = relevance[j].length - 1;
                    if (results.getNumber() == (j + 1) && results.getSiteId() == i) {
                        results.setRelevance(relevance[j][ind]);
                    }
                }
            }
        }
        searchResultsList.addAll(searchResultsListTemp);
    }

    private void sortSearchResultsList(int offset, int limit,
                                       List<SearchResults> searchResultsList) {

        List<SearchResults> searchResultsListTemp;

        searchResultsListTemp = searchResultsList.stream()
                .filter(searchResults -> searchResults.getRelevance() != 0.0)
                .sorted(Comparator
                        .comparing(SearchResults::getRelevance)
                        .reversed())
                .limit(limit)
                .skip(offset)
                .toList();

        searchResultsList.addAll(searchResultsListTemp);

    }

    private void setSnippetForSearchResults(List<Lemma> lemmaList,
                                            List<SearchResults> searchResultsList) {
        Iterator<SearchResults> iteratorSR = searchResultsList.iterator();
        SearchResults results;
        while (iteratorSR.hasNext()) {
            results = iteratorSR.next();
            Page page = pageRepository.findByPageId(results.getPageId());
            results.setTitle(page.getTitle());
            results.setUrl(page.getPath());

            String snippet = snippetFormatter.getSnippet(page.getContent(), lemmaList);
            results.setSnippet(snippet);
        }
    }

    private Response setSearchData(List<SearchResults> searchResultsList) {
        List<SearchData> searchDataList = new ArrayList<>();
        SearchResponse responseTrue = new SearchResponse();
        responseTrue.setError("");
        responseTrue.setResult(true);
        responseTrue.setCount(countSearch);
        for (SearchResults searchResults : searchResultsList) {
            SiteEntity siteEntity = siteRepository.getSiteEBySiteId(searchResults.getSiteId());
            String uri = searchResults.getUrl().endsWith("/") ? searchResults.getUrl()
                    .substring(0, searchResults.getUrl().length() - 1) : searchResults.getUrl();
            SearchData searchData = new SearchData(siteEntity.getUrl(),
                    siteEntity.getName(),
                    uri,
                    searchResults.getTitle(),
                    searchResults.getSnippet(),
                    searchResults.getRelevance());
            searchDataList.add(searchData);
            log.info("сайт {} релевантность {}", siteEntity.getUrl() + uri, searchData.getRelevance());
        }
        System.err.println();
        responseTrue.setData(searchDataList);
        return responseTrue;
    }

    private Response setResponseFalse(String errorMessage) {
        log.warn(errorMessage);

        List<SearchData> searchDataList = new ArrayList<>();
        SearchResponse response = new SearchResponse();
        response.setError(errorMessage);
        response.setResult(true);
        response.setCount(0);
        SearchData searchData = new SearchData("", "", "", "", "", 0);
        searchDataList.add(searchData);
        response.setData(searchDataList);

        return response;
    }
}