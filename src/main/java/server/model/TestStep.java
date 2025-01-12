package server.model;

import jakarta.persistence.*;

@Entity
@Table(name = "test_steps")
public class TestStep {
    public TestStep(Long id, Long testId, Integer stepNumber, String action, String button, String location, String details) {
        this.id = id;
        this.testId = testId;
        this.stepNumber = stepNumber;
        this.action = action;
        this.button = button;
        this.location = location;
        this.details = details;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "test_id", nullable = false)
    private Long testId;

    @Column(name = "step_number", nullable = false)
    private Integer stepNumber;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "button", nullable = true)
    private String button;

    @Column(name = "location", nullable = true)
    private String location;

    @Column(name = "details", nullable = true)
    private String details;

    public TestStep() {

    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTestId() {
        return testId;
    }

    public void setTestId(Long testId) {
        this.testId = testId;
    }

    public Integer getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(Integer stepNumber) {
        this.stepNumber = stepNumber;
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
}
