package io.lcalmsky.app.account.domain.entity;

import io.lcalmsky.app.domain.entity.AuditingEntity;
import io.lcalmsky.app.settings.controller.NotificationForm;
import lombok.*;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder @Getter @ToString
public class Account extends AuditingEntity {

    @Id @GeneratedValue
    @Column(name = "account_id")
    private Long id;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String nickname;

    private String password;

    private boolean isValid;

    private String emailToken;

    private LocalDateTime joinedAt;

    @Embedded
    private Profile profile = new Profile();

    @Embedded
    private NotificationSetting notificationSetting = new NotificationSetting();

    private LocalDateTime emailTokenGeneratedAt;

    public void generateToken() {
        this.emailToken = UUID.randomUUID().toString();
        this.emailTokenGeneratedAt = LocalDateTime.now();
    }

    public boolean enableToSendEmail() {
        return this.emailTokenGeneratedAt.isBefore(LocalDateTime.now().minusMinutes(5));
    }

    public void verified() {
        this.isValid = true;
        joinedAt = LocalDateTime.now();
    }

    @PostLoad
    private void init() {
        if (profile == null) {
            profile = new Profile();
        }
        if (notificationSetting == null) {
            notificationSetting = new NotificationSetting();
        }
    }

    public void updateProfile(io.lcalmsky.app.settings.controller.Profile profile) {
        if (this.profile == null) {
            this.profile = new Profile();
        }
        this.profile.bio = profile.getBio();
        this.profile.url = profile.getUrl();
        this.profile.job = profile.getJob();
        this.profile.location = profile.getLocation();
        this.profile.image = profile.getImage();
    }

    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }

    public void updateNotification(NotificationForm notificationForm) {
        this.notificationSetting.studyCreatedByEmail = notificationForm.isStudyCreatedByEmail();
        this.notificationSetting.studyCreatedByWeb = notificationForm.isStudyCreatedByWeb();
        this.notificationSetting.studyUpdatedByWeb = notificationForm.isStudyUpdatedByWeb();
        this.notificationSetting.studyUpdatedByEmail = notificationForm.isStudyUpdatedByEmail();
        this.notificationSetting.studyRegistrationResultByEmail = notificationForm.isStudyRegistrationResultByEmail();
        this.notificationSetting.studyRegistrationResultByWeb = notificationForm.isStudyRegistrationResultByWeb();
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public boolean isValid(String token) {
        return this.emailToken.equals(token);
    }

    @Embeddable
    @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)
    @Builder @Getter @ToString
    public static class Profile {
        private String bio;
        private String url;
        private String job;
        private String location;
        private String company;

        @Lob @Basic(fetch = FetchType.EAGER)
        private String image;
    }

    @Embeddable
    @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)
    @Builder @Getter @ToString
    public static class NotificationSetting {
        private boolean studyCreatedByEmail = false;
        private boolean studyCreatedByWeb = true;
        private boolean studyRegistrationResultByEmail = false;
        private boolean studyRegistrationResultByWeb = true;
        private boolean studyUpdatedByEmail = false;
        private boolean studyUpdatedByWeb = true;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        Account account = (Account) o;
        return id != null && Objects.equals(id, account.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}