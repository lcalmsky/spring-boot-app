package io.lcalmsky.app.settings.controller;

import io.lcalmsky.app.account.domain.entity.Account;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Profile {
    private String bio;
    private String url;
    private String job;
    private String location;

    public static Profile from(Account account) {
        return new Profile(account);
    }

    protected Profile(Account account) {
        this.bio = Optional.ofNullable(account.getProfile()).map(Account.Profile::getBio).orElse(null);
        this.job = Optional.ofNullable(account.getProfile()).map(Account.Profile::getJob).orElse(null);
        this.url = Optional.ofNullable(account.getProfile()).map(Account.Profile::getUrl).orElse(null);
        this.location = Optional.ofNullable(account.getProfile()).map(Account.Profile::getLocation).orElse(null);
    }
}
