package searchengine.utills.parsing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexEntity;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.utills.lemma.LemmaFinder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static searchengine.utills.parsing.sitemapping.Utils.*;

@Slf4j
@Getter
@Setter
@Component
@RequiredArgsConstructor
public class ParseLemma {

    private int beginPos;

    private int endPos;

    private int currentPos;

    private final LemmaRepository lemmaRepository;

    private final IndexRepository indexRepository;

    @Transactional
    public void parsing(Page page) {

        String content = page.getContent();
        int siteId = page.getSiteId();
        int pageId = page.getPageId();

        try {
            LemmaFinder lemmaFinder = LemmaFinder.getInstance();
            assert lemmaFinder != null;
            Map<String, Integer> mapLemmas = lemmaFinder.collectLemmas(content);
            Map<Lemma, Integer> mapLemmasForAdd = new HashMap<>();
            mapLemmas.forEach((key, value1) -> mapLemmasForAdd.put(parseOneLemma(siteId, key), value1));

            lemmaRepository.saveAll(mapLemmasForAdd.keySet());

            List<IndexEntity> listIndexForAdd = new ArrayList<>();
            mapLemmasForAdd.forEach((key, value1) -> listIndexForAdd.add(new IndexEntity(pageId, key.getLemmaId(), value1)));
            indexRepository.saveAll(listIndexForAdd);

            printMessageAboutProgress(siteId, pageId, mapLemmasForAdd.size(), page.getPath());

        } catch (Exception e) {
            log.error("Ошибка parsing lemmas: {} siteId: {} pageId: {}", content.substring(0, 50) + "...", siteId, pageId);
        }
    }


    private void printMessageAboutProgress(int siteId, int pageId, int countOfLemmas, String url) {
        if ((endPos - beginPos) == 0) {
            log.info("Writing lemmas and indices: {} ", countOfLemmas);
            return;
        }
        String builder = "Writing lemmas and indices: " + ANSI_GREEN + (currentPos - beginPos) * 100 / (endPos - beginPos) + "% " +
                ANSI_RESET + " siteId:" + ANSI_CYAN + siteId + ANSI_RESET +
                " pageId: " + ANSI_CYAN + pageId + ANSI_RESET +
                " number of lemmas: " + ANSI_CYAN + countOfLemmas + ANSI_RESET +
                " url: " + ANSI_BLUE + url + ANSI_RESET;
        System.err.print(builder + "\r");
    }


    private Lemma parseOneLemma(int siteId, String key) {
        Lemma result = lemmaRepository.findBySiteIdAndLemma(siteId, key).orElse(null);
        if (result == null) {
            result = new Lemma(siteId, key, 1);
        } else {
            result.setFrequency(result.getFrequency() + 1);
        }
        return result;
    }
}