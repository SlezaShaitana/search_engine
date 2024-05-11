package searchengine.converter;

import org.springframework.stereotype.Component;
import searchengine.dto.indexing.IndexDto;
import searchengine.dto.indexing.LemmaDto;
import searchengine.dto.indexing.PageDto;
import searchengine.dto.indexing.SiteDto;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

@Component
public class Mapper {

    public static SiteEntity mapToSiteEntity(SiteDto siteDto) {
        SiteEntity siteEntity = new SiteEntity();
        siteEntity.setId(siteDto.getId());
        siteEntity.setStatus(siteDto.getStatus());
        siteEntity.setStatusTime(siteDto.getStatusTime());
        siteEntity.setLastError(siteDto.getLastError());
        siteEntity.setUrl(siteDto.getUrl());
        siteEntity.setName(siteDto.getName());
        return siteEntity;
    }

    public static SiteDto mapToSiteDto(SiteEntity siteEntity) {
        SiteDto siteDto = new SiteDto();
        siteDto.setId(siteEntity.getId());
        siteDto.setStatus(siteEntity.getStatus());
        siteDto.setStatusTime(siteEntity.getStatusTime());
        siteDto.setLastError(siteEntity.getLastError());
        siteDto.setUrl(siteEntity.getUrl());
        siteDto.setName(siteEntity.getName());
        return siteDto;
    }


    public static PageEntity mapToPageEntity(PageDto pageDto) {
        PageEntity pageEntity = new PageEntity();
        pageEntity.setId(pageDto.getId());
        pageEntity.setSites(pageDto.getSites());
        pageEntity.setPath(pageDto.getPath());
        pageEntity.setCode(pageDto.getCode());
        pageEntity.setContent(pageDto.getContent());
        return pageEntity;
    }

    public static PageDto mapToPageDto(PageEntity pageEntity) {
        PageDto pageDto = new PageDto();
        pageDto.setId(pageEntity.getId());
        pageDto.setSites(pageEntity.getSites());
        pageDto.setPath(pageEntity.getPath());
        pageDto.setCode(pageEntity.getCode());
        pageDto.setContent(pageEntity.getContent());
        return pageDto;
    }

    public static LemmaDto mapToLemmaDto(LemmaEntity lemmaEntity) {
        LemmaDto lemmaDto = new LemmaDto();
        lemmaDto.setId(lemmaEntity.getId());
        lemmaDto.setSites(lemmaEntity.getSites());
        lemmaDto.setLemma(lemmaEntity.getLemma());
        lemmaDto.setFrequency(lemmaEntity.getFrequency());
        return lemmaDto;
    }

    public static LemmaEntity mapToLemmaEntity(LemmaDto lemmaDto) {
        LemmaEntity lemmaEntity = new LemmaEntity();
        lemmaEntity.setId(lemmaDto.getId());
        lemmaEntity.setSites(lemmaDto.getSites());
        lemmaEntity.setLemma(lemmaDto.getLemma());
        lemmaEntity.setFrequency(lemmaDto.getFrequency());
        return lemmaEntity;
    }

    public static IndexDto mapToIndexDto(IndexEntity indexEntity) {
        IndexDto indexDto = new IndexDto();
        indexDto.setId(indexEntity.getId());
        indexDto.setPages(indexEntity.getPageId());
        indexDto.setLemma(indexEntity.getLemmaId());
        indexDto.setRank(indexEntity.getRank());
        return indexDto;
    }

    public static IndexEntity mapToIndexEntity(IndexDto indexDto) {
        IndexEntity indexEntity = new IndexEntity();
        indexEntity.setId(indexDto.getId());
        indexEntity.setPageId(indexDto.getPages());
        indexEntity.setLemmaId(indexDto.getLemma());
        indexEntity.setRank(indexDto.getRank());
        return indexEntity;
    }
}
