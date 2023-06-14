package searchengine.services.search;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.exceptions.BadRequestException;
import searchengine.dto.exceptions.NotFoundException;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.util.HTMLParser;
import searchengine.services.indexing.IndexingServiceImp;
import searchengine.services.lemma.LemmaService;

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
    private final PageRepository pageRepository;
    private final HTMLParser htmlParser;
    private final SnippetCreator snippetCreator;
    private final IndexingServiceImp indexingServiceImp;

    @Override
    public SearchResponse search(String query, String site, Integer offset, Integer limit) throws IOException {
        SearchResponse response = new SearchResponse();
        LemmaService lemmaService = LemmaService.getInstance();
        if (query == null || query.length() == 0) {
            throw new BadRequestException("Задан пустой поисковый запрос");
        }
        if (siteRepository.findAll().size() == 0) {
            indexingServiceImp.addSitesToRepo(sites);
        }
        List<SearchData> searchData = new ArrayList<>();
        try {
            Map<String, Integer> lemmas = lemmaService.getLemmas(query);
            if (lemmas.size() == 0) {
                throw new BadRequestException("Не обнаружено лемм для поиска");
            }
            if (site != null && site.length() > 0) {
                SiteEntity siteEntity = siteRepository.findByUrl(site);
                searchData = getSearchData(siteEntity, lemmas);
            } else {
                for (SiteEntity siteEntity : siteRepository.findAll()) {
                    searchData.addAll(getSearchData(siteEntity, lemmas));
                }
            }
            response.setResult(true);
            response.setCount(searchData.size());
            response.setData(getSubList(searchData, offset, limit));
        } catch (IOException e) {
            throw new NotFoundException("Указанная страница не найдена");
        }
        return response;
    }

    private List<SearchData> getSearchData(SiteEntity siteEntity, Map<String, Integer> lemmas) throws IOException {
        List<LemmaEntity> sortedLemmaList = getSortedLemmaList(siteEntity, lemmas);
        if (sortedLemmaList.size() == 0) {
            return Collections.emptyList();
        }
        Set<PageEntity> pages = getPages(sortedLemmaList);
        if (pages == null || pages.size() == 0) {
            return Collections.emptyList();
        }
        Double maxRank = getMaxRank(pages, sortedLemmaList);
        List<SearchData> searchData;
        if (maxRank == null) {
            searchData = List.of();
        } else {
            searchData = new ArrayList<>();
            Map<PageEntity, Double> pageRank = getPageRank(pages, sortedLemmaList);
            for (Map.Entry<PageEntity, Double> entry : pageRank.entrySet()) {
                PageEntity page = entry.getKey();
                Double rank = entry.getValue();
                String content = LemmaService.removeTagsFromText(page.getContent());
                SearchData data = new SearchData(siteEntity.getUrl(), siteEntity.getName(), page.getPath(),
                        htmlParser.getTitle(page.getContent()),
                        snippetCreator.createSnippet(content, lemmas.keySet()),
                        (rank / maxRank));
                searchData.add(data);
            }
            searchData.sort((a, b) -> Double.compare(b.relevance(), a.relevance()));
        }
        return searchData;
    }

    private Double sumRank(PageEntity page, Set<LemmaEntity> lemmaEntitySet) {
        return indexRepository
                .findAllByPageEntityAndLemmaEntityIn(page, lemmaEntitySet)
                .stream().mapToDouble(IndexEntity::getRank)
                .sum();
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
        Set<PageEntity> pages = indexRepository
                .findAllByLemmaEntityId(lemmaEntityList.get(0).getId())
                .stream()
                .map(IndexEntity::getPageEntity)
                .collect(Collectors.toSet());
        if (pages.isEmpty()) {
            return Set.of();
        }
        for (int i = 1; i < lemmaEntityList.size(); i++) {
            Set<IndexEntity> set = indexRepository.findAllByLemmaEntityAndPageEntityIn(lemmaEntityList.get(i), pages);
            if (set == null) {
                return pages;
            }
            pages = set.stream()
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

    private List<LemmaEntity> getSortedLemmaList(SiteEntity siteEntity, Map<String, Integer> lemmas) {
        double threshold = pageRepository.count() * 0.75;
        List<LemmaEntity> lemmaEntityList = new ArrayList<>();
        lemmas.keySet().forEach(lemma -> {
            LemmaEntity lemmaEntity = lemmaRepository.findByLemmaAndSiteEntityId(lemma, siteEntity.getId());
            if (lemmaEntity != null && lemmaEntity.getFrequency() < threshold) {
                lemmaEntityList.add(lemmaEntity);
            }
        });
        lemmaEntityList.sort(Comparator
                .comparingInt(LemmaEntity::getFrequency)
                .reversed()
                .thenComparing(LemmaEntity::getLemma));
        return lemmaEntityList;
    }
}
