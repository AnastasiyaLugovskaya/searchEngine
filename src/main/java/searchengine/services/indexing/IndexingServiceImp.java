package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.JsoupConfiguration;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.exceptions.BadRequestException;
import searchengine.dto.exceptions.NotFoundException;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.Parser;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.util.HTMLParser;
import searchengine.services.lemma.LemmaParser;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor

public class IndexingServiceImp implements IndexingService {
    private final SitesList sites;
    @Autowired
    private final SiteRepository siteRepository;
    @Autowired
    private final PageRepository pageRepository;
    @Autowired
    private final LemmaRepository lemmaRepository;
    @Autowired
    private final IndexRepository indexRepository;
    private final JsoupConfiguration jsoupConfig;
    private final HTMLParser htmlParser;
    public static volatile boolean isIndexingStopped = false;

    @Override
    public IndexingResponse startIndexing() {
        IndexingResponse response = new IndexingResponse();
        boolean isIndexingStarted = false;
        if (siteRepository.countByStatus(Status.INDEXING) > 0) {
            throw new BadRequestException("Индексация уже запущена");
        }
        cleanDB();
        addSitesToRepo(sites);
        for (SiteEntity siteEntity : siteRepository.findAll()) {
            indexOneSite(siteEntity, "/", siteRepository, pageRepository, htmlParser, jsoupConfig);
            isIndexingStarted = true;
        }
        response.setResult(isIndexingStarted);
        response.setError("");
        return response;
    }

    @Override
    public IndexingResponse stopIndexing() {
        IndexingResponse response = new IndexingResponse();
        if (siteRepository.countByStatus(Status.INDEXING) == 0) {
            throw new BadRequestException("Индексация не запущена");
        } else {
            isIndexingStopped = true;
            response.setResult(true);
            response.setError("");
        }
        return response;
    }

    @Override
    public IndexingResponse indexPage(String url) throws Exception {
        IndexingResponse response = new IndexingResponse();
        LemmaParser lemmaParser = new LemmaParser(lemmaRepository, indexRepository);
        url = url.toLowerCase();
        Site siteToMatch = null;
        for (Site site : sites.getSites()){
            if (url.contains(site.getUrl())){
                siteToMatch = site;
            }
        }
        if (siteToMatch == null){
            throw new NotFoundException("Данная страница находится за пределами сайтов, " +
                    "указанных в конфигурационном файле");
        }

        String root = siteToMatch.getUrl();
        String relativeUrl = (url.equals(root)) ? "/" : url.replace(root, "");
        SiteEntity siteEntity = siteRepository.findByUrl(root);

        if (siteEntity == null){
            addOneSiteToRepo(siteToMatch);
            siteEntity = siteRepository.findByUrl(root);
        }
        deletePage(siteEntity.getId(), relativeUrl, root);

        Parser parser = new Parser(siteEntity.getId(), relativeUrl,siteRepository,pageRepository,
                lemmaRepository, indexRepository, htmlParser, jsoupConfig);
        Optional<PageEntity> optionalPage = parser.savePage(siteEntity, relativeUrl);
        if (optionalPage.isPresent()) {
            PageEntity page = optionalPage.get();
            lemmaParser.parseOnePage(page);
        }
        setStatusAfterIndexing(parser, siteEntity);
        response.setResult(true);
        response.setError("");
        return response;
    }
    public void cleanDB(){
        indexRepository.deleteAllInBatch();
        lemmaRepository.deleteAllInBatch();
        pageRepository.deleteAllInBatch();
        siteRepository.deleteAllInBatch();
    }
    private void indexOneSite(SiteEntity siteEntity, String url, SiteRepository siteRepository, PageRepository pageRepository,
                                HTMLParser htmlParser, JsoupConfiguration jsoupConfig) {
        new Thread(() -> {
            Parser pageParser = new Parser(
                            siteEntity.getId(), url, siteRepository, pageRepository, lemmaRepository,
                            indexRepository, htmlParser, jsoupConfig);
                    ForkJoinPool forkJoinPool = new ForkJoinPool();
                    forkJoinPool.invoke(pageParser);
                    setStatusAfterIndexing(pageParser, siteEntity);
                }).start();
    }
    public void addSitesToRepo(SitesList sites){
        for (Site s : sites.getSites()) {
            addOneSiteToRepo(s);
        }
    }
    public void addOneSiteToRepo(Site s){
        SiteEntity entity = new SiteEntity();
        entity.setName(s.getName());
        entity.setUrl(s.getUrl().toLowerCase());
        entity.setStatus(Status.INDEXING);
        entity.setStatusTime(System.currentTimeMillis());

        siteRepository.save(entity);
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void deletePage(int siteEntityId, String path, String siteUrl) {
        Optional<PageEntity> optPage = Optional.ofNullable(pageRepository.findBySiteEntityIdAndPath(siteEntityId, path));
        Set<LemmaEntity> lemmasToSave = new HashSet<>();
        if (optPage.isPresent()){
            PageEntity page = optPage.get();
            Set<IndexEntity> indexSet =  indexRepository.findAllByPageEntity(page);
            indexSet.forEach(indexEntity -> {
                LemmaEntity lemma = indexEntity.getLemmaEntity();
                lemma.setFrequency(lemma.getFrequency() - 1);
                lemmasToSave.add(lemma);
            });
            indexRepository.deleteALLByPageEntity(page);
            pageRepository.delete(page);
            lemmaRepository.saveAll(lemmasToSave);
        }
    }
    private void setStatusAfterIndexing(Parser pageParser, SiteEntity siteEntity){
        if (isIndexingStopped) {
            pageParser.cancel(true);
            pageParser.updateSiteInfo(siteEntity, Status.FAILED, "Индексация остановлена пользователем");
        }
        if (siteEntity.getStatus() != Status.FAILED && !isIndexingStopped) {
            pageParser.updateSiteInfo(siteEntity, Status.INDEXED, Parser.getLastErrors().get(siteEntity.getId()));
        }
    }
}
