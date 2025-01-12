package server.repository;

import server.model.TestStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestStepRepository extends JpaRepository<TestStep, Long> {
    List<TestStep> findByTestId(Long testId);
}
