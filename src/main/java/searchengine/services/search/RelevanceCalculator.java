package searchengine.services.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import searchengine.model.IndexEntity;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SearchResults;
import searchengine.repository.IndexRepository;
import searchengine.repository.PageRepository;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;


@Slf4j
@Component
@RequiredArgsConstructor
public class RelevanceCalculator {

    private final PageRepository pageRepository;

    private final IndexRepository indexRepository;

    public double[][] formationForOneSite(List<Lemma> lemmaList, int offset, int limit,
                                          List<SearchResults> searchResultsList) {
        List<SearchResults> searchResultsListTemp = new ArrayList<>();
        double[][] relevance;
        double maxRelevance;

        List<IndexEntity> indexList = getIndexEForFirstLemma(lemmaList, offset, limit);

        List<Page> pageList = getPageList(indexList);

        relevance = getRelevanceArray(indexList, lemmaList, pageList, searchResultsListTemp);

        maxRelevance = setAbsoluteRelevance(pageList, lemmaList, relevance);
        setRelativeRelevance(pageList, lemmaList, relevance, maxRelevance);

        searchResultsList.addAll(searchResultsListTemp);
        return relevance;
    }

    private List<Page> getPageList(List<IndexEntity> indexList) {
        List<Page> pageList = new ArrayList<>();
        for (IndexEntity indexEntity : indexList) {
            Page page = pageRepository.findByPageId(indexEntity.getPageId());
            if (page != null) {
                pageList.add(page);
            }
        }
        return pageList;
    }

    private List<IndexEntity> getIndexEForFirstLemma(List<Lemma> lemmaList, int offset, int limit) {
        List<IndexEntity> indexList = new ArrayList<>(Objects
                .requireNonNull(indexRepository.findByLemmaId(lemmaList.get(0).getLemmaId())
                        .orElse(null)).stream()
                .skip(offset)
                .limit(limit)
                .toList());

        Iterator<IndexEntity> iter = indexList.iterator();
        while (iter.hasNext()) {
            IndexEntity indexEntity = iter.next();
            int pageId = indexEntity.getPageId();
            int i = 1;
            while (i < lemmaList.size()) {
                IndexEntity indexEntity1 = indexRepository.findByLemmaIdAndPageId(lemmaList.get(i).getLemmaId(), pageId)
                        .orElse(null);
                if (indexEntity1 == null) {
                    iter.remove();
                    break;
                }
                i++;
            }
        }
        return indexList;
    }

    private void setRelativeRelevance(List<Page> pageList, List<Lemma> lemmaList,
                                      double[][] relevance, double maxRelevance) {
        for (int j = 0; j < pageList.size(); j++) {
            int ind = lemmaList.size() + 2;
            relevance[j][ind] = relevance[j][ind - 1] / maxRelevance;
        }
    }

    private double setAbsoluteRelevance(List<Page> pageList, List<Lemma> lemmaList,
                                        double[][] relevance) {
        double maxRelevance = 0;
        for (int j = 0; j < pageList.size(); j++) {
            for (int k = 0; k < lemmaList.size() + 3; k++) {
                if (k == lemmaList.size() + 1) {
                    int sumAR = 0;
                    for (int l = 0; l < lemmaList.size(); l++) {
                        sumAR += (int) relevance[j][l + 1];
                    }
                    relevance[j][k] = sumAR;
                    maxRelevance = Double.max(maxRelevance, sumAR);
                }
            }
        }
        return maxRelevance;
    }

    private double[][] getRelevanceArray(List<IndexEntity> indexList, List<Lemma> lemmaList,
                                         List<Page> pageList, List<SearchResults> searchResultsListTemp) {

        double[][] relevance = new double[indexList.size()][lemmaList.size() + 3];
        for (int j = 0; j < indexList.size(); j++) {
            for (int k = 0; k < 2; k++) {
                if (k == 0) {
                    relevance[j][k] = j + 1.0;
                    SearchResults searchResults = new SearchResults();
                    searchResults.setNumber(j + 1);
                    searchResults.setSiteId(lemmaList.get(0).getSiteId());
                    searchResults.setPageId(indexList.get(j).getPageId());
                    searchResultsListTemp.add(searchResults);
                    continue;
                }
                relevance[j][k] = indexList.get(j).getRank();
            }
        }


        findingLemmaMatchFromListOfPages(lemmaList, pageList, relevance);

        return relevance;
    }

    private void findingLemmaMatchFromListOfPages(List<Lemma> lemmaList, List<Page> pageList, double[][] relevance) {
        int i = 1;
        while (i < lemmaList.size()) {
            int lemmaId = lemmaList.get(i).getLemmaId();
            List<IndexEntity> indexList2 = new ArrayList<>(
                    Objects.requireNonNull(indexRepository.findByLemmaId(lemmaId).orElse(null)));

            if (!pageList.removeIf(page -> indexList2.stream()
                    .noneMatch(indexE -> indexE.getPageId() == page.getPageId()))) {
                indexList2.removeIf(indexE -> pageList.stream()
                        .noneMatch(page -> page.getPageId() == indexE.getPageId()));
                for (int j = 0; j < indexList2.size(); j++) {
                    relevance[j][i + 1] = indexList2.get(j).getRank();
                }
            }
            i++;
        }
    }
}