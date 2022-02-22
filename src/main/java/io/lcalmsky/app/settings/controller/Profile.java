package io.lcalmsky.app.settings.controller;

import io.lcalmsky.app.account.domain.entity.Account;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.util.Optional;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Profile {
    @Length(max = 35)
    private String bio;
    @Length(max = 50)
    private String url;
    @Length(max = 50)
    private String job;
    @Length(max = 50)
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
