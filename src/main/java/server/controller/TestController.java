package server.controller;

import server.model.Test;
import server.model.TestStep;
import server.repository.TestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.repository.TestStepRepository;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/tests")
public class TestController {

    @Autowired
    private TestRepository testRepository;

    @Autowired
    private TestStepRepository testStepRepository;

    @GetMapping("/")
    public List<Test> getTests() {
        return testRepository.findAll();
    }

    @PostMapping("/")
    public Test createTest(@RequestBody Test test) {
        return testRepository.save(test);
    }

    @GetMapping("/{id}/steps")
    public List<TestStep> getTestSteps(@PathVariable Long id) {
        return testStepRepository.findByTestId(id);
    }

    @PostMapping("/{id}/steps")
    public TestStep createTestStep(@PathVariable Long id, @RequestBody TestStep step) {
        step.setTestId(id);
        return testStepRepository.save(step);
    }

    @DeleteMapping("/{id}/steps")
    public ResponseEntity<Void> deleteTestSteps(@PathVariable Long id) {
        List<TestStep> steps = testStepRepository.findByTestId(id);
        if (steps != null && !steps.isEmpty()) {
            testStepRepository.deleteAll(steps);
        }
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTest(@PathVariable Long id) {
        if (testRepository.existsById(id)) {
            testRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Test> updateTest(@PathVariable Long id, @RequestBody Test updatedTest) {
        Optional<Test> optionalTest = testRepository.findById(id);
        if (optionalTest.isPresent()) {
            Test existingTest = optionalTest.get();
            existingTest.setName(updatedTest.getName());
            existingTest.setDescription(updatedTest.getDescription());
            existingTest.setUrl(updatedTest.getUrl());
            Test savedTest = testRepository.save(existingTest);
            return ResponseEntity.ok(savedTest);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

}