package greencloud.impl.modules.utility;

import greencloud.impl.managers.notification.NotificationManager;
import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;

public class IRCModule extends Module {

    public IRCModule() {
        super("IRC", "Allows you to chat in the IRC channel.", Category.UTILITY);
    }

    @Override
    public void onEnable() {
        toggle();
        NotificationManager.getInstance().addNotification(
                "IRC",
                "IRC does not work rn...",
                NotificationManager.NotificationType.ERROR,
                3000
        );
    }
}
