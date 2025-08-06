package searchengine.utills.lemma;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
public class LemmaFinder {

    private final LuceneMorphology morphologyRus;

    private final LuceneMorphology morphologyEng;

    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ", "CONJ",
            "INT", "PREP", "ARTICLE", "PART"};

    public static LemmaFinder getInstance() {
        try {
            LuceneMorphology morphologyRus = new RussianLuceneMorphology();
            LuceneMorphology morphologyEng = new EnglishLuceneMorphology();
            return new LemmaFinder(morphologyRus, morphologyEng);
        } catch (IOException e) {
            log.error(e.toString());
        }
        return null;
    }

    private LemmaFinder(LuceneMorphology luceneMorphologyRus,
                        LuceneMorphology luceneMorphologyEng) {
        this.morphologyRus = luceneMorphologyRus;
        this.morphologyEng = luceneMorphologyEng;
    }

    private LemmaFinder() {
        throw new IllegalArgumentException("Disallow construct");
    }

    public Map<String, Integer> collectLemmas(String text) {
        String[] words = splitWords(text, false);

        HashMap<String, Integer> lemmas = new HashMap<>();
        for (String word : words) {
            String normalWord = getNormalWord(word);
            if (normalWord == null) {
                continue;
            }
            lemmas.put(normalWord, lemmas.getOrDefault(normalWord, 0) + 1);
        }
        return lemmas;
    }

    private String getNormalWord(String word) {
        LuceneMorphology luceneMorphology;
        if (word.isBlank()) {
            return null;
        }
        if (isRussian(word)) {
            luceneMorphology = morphologyRus;
        } else {
            luceneMorphology = morphologyEng;
        }
        try {
            List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
            if (anyWordBaseBelongToParticle(wordBaseForms)) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }

        List<String> normalForms = luceneMorphology.getNormalForms(word);
        if (normalForms.isEmpty()) {
            return null;
        }

        return normalForms.toString();
    }

    private String[] splitWords(String text, boolean saveOriginal) {
        String[] split;
        if (saveOriginal) {
            split = text.toLowerCase(Locale.ROOT)
                    .trim()
                    .split("\\s+");

        } else {
            split = text.toLowerCase(Locale.ROOT)
                    .replaceAll("([^а-яa-z\\s])", " ")
                    .trim()
                    .split("\\s+");
        }
        return split;
    }

    private static boolean isRussian(String word) {
        return word.chars()
                .mapToObj(Character.UnicodeBlock::of)
                .anyMatch(Character.UnicodeBlock.CYRILLIC::equals);
    }

    private boolean hasParticleProperty(String wordBase) {
        for (String property : particlesNames) {
            if (wordBase.toUpperCase().contains(property)) {
                return true;
            }
        }
        return false;
    }

    private boolean anyWordBaseBelongToParticle(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
    }
}