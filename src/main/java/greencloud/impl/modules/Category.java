package greencloud.impl.modules;

public enum Category {
    COMBAT("Combat"),
    RENDER("Render"),
    MOVEMENT("Movement"),
    UTILITY("Utility"),
    MISC("Misc");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}