package io.lcalmsky.app.study.domain.entity;

import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.account.domain.entity.Zone;
import io.lcalmsky.app.tag.domain.entity.Tag;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Study {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToMany
    private Set<Account> managers;

    @ManyToMany
    private Set<Account> members;

    @Column(unique = true)
    private String path;

    private String title;

    private String shortDescription;

    @Lob @Basic(fetch = FetchType.EAGER)
    private String fullDescription;

    @Lob @Basic(fetch = FetchType.EAGER)
    private String image;

    @ManyToMany
    private Set<Tag> tags;

    @ManyToMany
    private Set<Zone> zones;

    private LocalDateTime publishedDateTime;

    private LocalDateTime closedDateTime;

    private LocalDateTime recruitingUpdatedDateTime;

    private boolean recruiting;

    private boolean published;

    private boolean closed;

    private boolean useBanner;
}
