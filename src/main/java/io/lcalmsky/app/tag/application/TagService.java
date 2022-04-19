package io.lcalmsky.app.tag.application;

import io.lcalmsky.app.tag.domain.entity.Tag;
import io.lcalmsky.app.tag.infra.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TagService {
    private final TagRepository tagRepository;

    public Tag findOrCreateNew(String tagTitle) {
        return tagRepository.findByTitle(tagTitle).orElseGet(
                () -> tagRepository.save(Tag.builder()
                        .title(tagTitle)
                        .build())
        );
    }
}
