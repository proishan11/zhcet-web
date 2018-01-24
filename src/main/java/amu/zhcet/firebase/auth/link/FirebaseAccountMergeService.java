package amu.zhcet.firebase.auth.link;

import amu.zhcet.auth.AuthManager;
import amu.zhcet.auth.UserAuth;
import amu.zhcet.core.notification.ChannelType;
import amu.zhcet.core.notification.Notification;
import amu.zhcet.core.notification.sending.NotificationSendingService;
import amu.zhcet.data.user.User;
import amu.zhcet.data.user.UserService;
import amu.zhcet.data.user.UserType;
import amu.zhcet.firebase.FirebaseService;
import com.google.common.base.Strings;
import com.google.firebase.auth.FirebaseToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.Optional;

@Slf4j
@Service
public class FirebaseAccountMergeService {

    private final FirebaseService firebaseService;
    private final UserService userService;
    private final NotificationSendingService notificationSendingService;

    @Autowired
    FirebaseAccountMergeService(FirebaseService firebaseService, UserService userService, NotificationSendingService notificationSendingService) {
        this.firebaseService = firebaseService;
        this.userService = userService;
        this.notificationSendingService = notificationSendingService;
    }

    /**
     * Merges firebase data into user account
     *
     * - Saves token claims in database
     * - Merges email and verification status from token
     * - Updates profile picture from provider data
     *
     * Mail merge and avatar update is done only if the user account information is not already present
     * @param userAuth User Account the data is to be merged in
     * @param token Decoded Firebase Token containing data
     */
    @Async
    public void mergeFirebaseDetails(@Nullable UserAuth userAuth, @Nullable FirebaseToken token) {
        if (userAuth == null || token == null || !firebaseService.canProceed())
            return;
        Optional<User> optionalUser = userService.findById(userAuth.getUsername());
        if (!optionalUser.isPresent())
            return;

        User user = optionalUser.get();

        if (token.getClaims() != null)
            user.getDetails().setFirebaseClaims(token.getClaims().toString());

        mergeMail(user, token);

        if (Strings.isNullOrEmpty(user.getDetails().getAvatarUrl()) && !Strings.isNullOrEmpty(token.getPicture())) {
            user.getDetails().setOriginalAvatarUrl(token.getPicture());
            user.getDetails().setAvatarUrl(token.getPicture());
            AuthManager.updateAvatar(userAuth, user.getDetails().getAvatarUrl());
        }

        userService.save(user);
    }

    /**
     * Merges mail from firebase token into user account
     *
     * - If the email of user is already verified, process is skipped
     * - If the email in provider is already claimed by another user, that user's email
     *   and it's verification status is cleared and given to the passed in user.
     *   Previous user is notified of this revocation
     * - If the provider email is not verified, but account email is null, provider email is saved.
     *   But verification status is set to false
     * @param user User
     * @param token Firebase Token containing email information
     */
    private void mergeMail(User user, FirebaseToken token) {
        if (Strings.isNullOrEmpty(token.getEmail()))
            return;

        Optional<User> duplicate = userService.getUserByEmail(token.getEmail());

        // Exchange user emails if someone else has access to the email provided
        if (duplicate.isPresent() && !duplicate.get().getUserId().equals(user.getUserId())) {
            User duplicateUser = duplicate.get();
            log.warn("Another user account with same email exists, {} {} : {}", user.getUserId(), duplicateUser.getUserId(), token.getEmail());

            if (token.isEmailVerified()) {
                log.warn("New user has verified email, unconditionally exchanging emails from previous user");

                duplicateUser.setEmail(null);
                duplicateUser.setEmailVerified(false);

                userService.save(duplicateUser);
                userService.findById(duplicateUser.getUserId()).ifPresent(dupe -> {
                    log.info("Cleared email info from duplicate user, {}", dupe.getEmail());
                });
                sendEmailChangeNotification(duplicateUser, user, token.getEmail());
            }

        }

        if (user.isEmailVerified() && user.getEmail() != null && !user.getEmail().equals(token.getEmail())) {
            log.info("User email is already verified, skipping mail merge");
            return;
        }

        if (token.isEmailVerified()) {
            user.setEmail(token.getEmail());
            user.setEmailVerified(true);
        } else if (Strings.isNullOrEmpty(user.getEmail())) {
            user.setEmail(token.getEmail());
            user.setEmailVerified(false);
        }
    }

    private void sendEmailChangeNotification(User recipient, User claimant, String email) {
        Notification notification = Notification.builder()
                .automated(true)
                .channelType(fromUser(recipient))
                .recipientChannel(recipient.getUserId())
                .sender(claimant)
                .title("Email Claimed")
                .message(String.format("Your previously set email **%s** is now claimed by `%s` *(%s)*.\n" +
                        "Please change it or claim it back by verifying it", email, claimant.getName(), claimant.getUserId()))
                .build();

        notificationSendingService.sendNotification(notification);
    }

    private ChannelType fromUser(User user) {
        return user.getType() == UserType.STUDENT ? ChannelType.STUDENT : ChannelType.FACULTY;
    }

}
