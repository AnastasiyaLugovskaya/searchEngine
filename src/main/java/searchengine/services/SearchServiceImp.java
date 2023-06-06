package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.exceptions.NotFoundException;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImp implements SearchService {
    private final SitesList sites;
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final HTMLParser htmlParser;

    private LemmaService lemmaService;
    private final SnippetCreator snippetCreator;
    private final IndexingServiceImp indexingServiceImp;

    @Override
    public SearchResponse search(String query, String site, Integer offset, Integer limit) throws IOException {
        SearchResponse response = new SearchResponse();
        lemmaService = LemmaService.getInstance();
        if (query == null || query.length() == 0) {
            response.setResult(false);
            response.setError("Задан пустой поисковый запрос");
        }
        if (siteRepository.findAll().size() == 0 ){
            indexingServiceImp.addSitesToRepo(sites);
        }
        List<SearchData> searchData = new ArrayList<>();
        try {
            Map<String, Integer> lemmas = lemmaService.getLemmas(query);
            if (lemmas.size() == 0) {
                response.setResult(false);
                response.setError("Не обнаружено лемм для поиска");
                return response;
            }
            if (site != null && site.length() > 0) {
                SiteEntity siteEntity = siteRepository.findByUrl(site);
                searchData = getSearchData(siteEntity, lemmas);
            } else {
                for (SiteEntity siteEntity : siteRepository.findAll()){
                    searchData.addAll(getSearchData(siteEntity, lemmas));
                }
            }
        response.setResult(true);
        response.setCount(searchData.size());
        response.setData(getSubList(searchData, offset, limit));
    } catch(
    IOException e)

    {
        throw new NotFoundException("Указанная страница не найдена");
    }
        return response;
}

    private Double sumRank(PageEntity page, Set<LemmaEntity> lemmaEntitySet) {
        Set<IndexEntity> indexEntities = indexRepository.findAllByPageEntityAndLemmaEntityIn(page, lemmaEntitySet);
        double sum = 0.0;
        for (IndexEntity indexEntity : indexEntities) {
            sum += indexEntity.getRank();
        }
        return sum;
    }

    private Double getMaxRank(Set<PageEntity> pages, List<LemmaEntity> lemmaEntityList) {
        Map<PageEntity, Double> pageRank = getPageRank(pages, lemmaEntityList);
        Double maxRank = null;
        for (Double rank : pageRank.values()) {
            if (maxRank == null || rank > maxRank) {
                maxRank = rank;
            }
        }
        return maxRank;
    }

    private Map<PageEntity, Double> getPageRank(Set<PageEntity> pages, List<LemmaEntity> lemmaEntityList) {
        Set<LemmaEntity> lemmaEntitySet = new HashSet<>(lemmaEntityList);
        Map<PageEntity, Double> pageRank = new HashMap<>();
        pageRank.putAll(pages.stream().collect(
                Collectors.toMap(Function.identity(), page -> sumRank(page, lemmaEntitySet))));
        return pageRank;
    }

    @Cacheable("myCache")
    private Set<PageEntity> getPages(List<LemmaEntity> lemmaEntityList) {
        if (lemmaEntityList.isEmpty()) return Set.of();
        Set<PageEntity> pages = indexRepository.findAllByLemmaEntity(lemmaEntityList.get(0));
        for (int i = 1; i < lemmaEntityList.size(); i++) {
            pages = indexRepository.findAllByLemmaEntityAndPageEntityIn(lemmaEntityList.get(i), pages).stream()
                    .map(IndexEntity::getPageEntity)
                    .collect(Collectors.toSet());
            if (pages.isEmpty()) {
                return pages;
            }
        }
        return pages;
    }

    private List<SearchData> getSubList(List<SearchData> searchData, Integer offset, Integer limit) {
        int fromIndex = offset;
        int toIndex = fromIndex + limit;

        if (toIndex > searchData.size()) {
            toIndex = searchData.size();
        }
        if (fromIndex > toIndex) {
            return List.of();
        }
        return searchData.subList(fromIndex, toIndex);
    }

    private List<SearchData> getSearchData(SiteEntity siteEntity, Map<String, Integer> lemmas) throws IOException {
        TreeMap<String, Integer> sortedLemmas = lemmaService.sortLemmas(lemmas);
        List<LemmaEntity> lemmaEntityList = new ArrayList<>();
        sortedLemmas.keySet().forEach(lemma -> {
            lemmaEntityList.add(lemmaRepository.findByLemmaAndSiteEntityId(lemma, siteEntity.getId()));
        });
        Set<PageEntity> pages = getPages(lemmaEntityList);

        Double maxRank = getMaxRank(pages, lemmaEntityList);
        List<SearchData> searchData;
        if (maxRank == null) {
            searchData = List.of();
        } else {
            searchData = new ArrayList<>();
            for (Map.Entry<PageEntity, Double> entry : getPageRank(pages, lemmaEntityList).entrySet()) {
                PageEntity page = entry.getKey();
                Double rank = entry.getValue();
                String content = lemmaService.removeTagsFromText(page.getContent());
                SearchData data = new SearchData(siteEntity.getUrl(), siteEntity.getName(), page.getPath(),
                        htmlParser.getTitle(content),
                        snippetCreator.createSnippet(content, lemmas.keySet()),
                        (rank / maxRank));
                searchData.add(data);
            }
            searchData.sort((a, b) -> Double.compare(b.relevance(), a.relevance()));
        }
        return searchData;
    }
}
