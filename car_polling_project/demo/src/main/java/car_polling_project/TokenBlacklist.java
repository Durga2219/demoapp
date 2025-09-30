package car_polling_project;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class TokenBlacklist {
    private Set<String> blacklistedTokens = new HashSet<>();

    public void add(String token) {
        blacklistedTokens.add(token);
    }

    public boolean contains(String token) {
        return blacklistedTokens.contains(token);
    }
}
