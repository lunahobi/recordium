package client;

public class Step {
    private String action;
    private String button;
    private String location;
    private String details;

    public Step() {
        // Конструктор по умолчанию для Jackson
    }

    public Step(String action, String button, String location, String details) {
        this.action = action;
        this.button = button;
        this.location = location;
        this.details = details;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getButton() {
        return button;
    }

    public void setButton(String button) {
        this.button = button;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    @Override
    public String toString() {
        return "Step{" +
                "action='" + action + '\'' +
                ", button='" + button + '\'' +
                ", location='" + location + '\'' +
                ", details='" + details + '\'' +
                '}';
    }
}
