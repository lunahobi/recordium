package server.model;

public class Step {
    private final String action;
    private final String button;
    private final String location;
    private final String details;

    public Step(String action, String button, String location, String details) {
        this.action = action;
        this.button = button;
        this.location = location;
        this.details = details;
    }

    public String getAction() {
        return action;
    }

    public String getButton() {
        return button;
    }

    public String getLocation() {
        return location;
    }

    public String getDetails() {
        return details;
    }
}