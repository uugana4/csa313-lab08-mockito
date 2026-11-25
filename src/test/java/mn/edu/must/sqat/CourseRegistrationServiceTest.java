package mn.edu.must.sqat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * CourseRegistrationService-ийн нэгжийн тест: хамаарлуудыг (StudentRepository, EmailSender)
 * Mockito-оор орлуулж, үйлчилгээний логикийг тусгаарлан шалгана.
 */
@ExtendWith(MockitoExtension.class)
class CourseRegistrationServiceTest {

    @Mock StudentRepository repository;
    @Mock EmailSender emailSender;

    CourseRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new CourseRegistrationService(repository, emailSender);
    }

    private static Student activeStudent(List<String> taken) {
        return new Student("S1", "s1@must.edu.mn", true, taken);
    }

    // A1: when + verify
    @Test
    @DisplayName("A1: идэвхтэй оюутан бүртгүүлэхэд OK, хадгалалт, и-мэйл")
    void register_activeStudent_returnsOkSavesAndSendsEmail() {
        when(repository.findStudent("S1")).thenReturn(activeStudent(List.of("CS201")));
        when(repository.courseExists("CS313")).thenReturn(true);
        when(repository.prerequisites("CS313")).thenReturn(List.of("CS201"));

        String result = service.register("S1", "CS313");

        assertEquals("OK", result);
        verify(repository).saveRegistration("S1", "CS313");
        verify(emailSender).send(eq("s1@must.edu.mn"), anyString(), contains("CS313"));
    }

    // A2: verify(..., never())
    @Test
    @DisplayName("A2: байхгүй оюутанд ERROR_NO_STUDENT, хадгалаагүй, и-мэйл яваагүй")
    void register_unknownStudent_returnsNoStudentAndDoesNotSaveOrEmail() {
        when(repository.findStudent("S1")).thenReturn(null);

        String result = service.register("S1", "CS313");

        assertEquals("ERROR_NO_STUDENT", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    // A3: verifyNoInteractions
    @Test
    @DisplayName("A3: идэвхгүй оюутанд ERROR_INACTIVE_STUDENT, и-мэйлтэй харьцаагүй")
    void register_inactiveStudent_returnsInactiveAndNeverTouchesEmail() {
        when(repository.findStudent("S1"))
                .thenReturn(new Student("S1", "s1@must.edu.mn", false, List.of()));

        String result = service.register("S1", "CS313");

        assertEquals("ERROR_INACTIVE_STUDENT", result);
        verifyNoInteractions(emailSender);
    }

    // A4: when + never
    @Test
    @DisplayName("A4: холбоо хичээл дутуу бол ERROR_PREREQUISITES, хадгалаагүй")
    void register_missingPrerequisite_returnsPrerequisitesAndDoesNotSave() {
        when(repository.findStudent("S1")).thenReturn(activeStudent(List.of()));
        when(repository.courseExists("CS313")).thenReturn(true);
        when(repository.prerequisites("CS313")).thenReturn(List.of("CS201"));

        String result = service.register("S1", "CS313");

        assertEquals("ERROR_PREREQUISITES", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
    }

    // A5: doThrow(...).when(...)
    @Test
    @DisplayName("A5: и-мэйл илгээгч exception шидсэн ч бүртгэл OK, хадгалалт хийгдсэн")
    void register_emailSenderThrows_stillReturnsOkAndRegistrationIsSaved() {
        when(repository.findStudent("S1")).thenReturn(activeStudent(List.of("CS201")));
        when(repository.courseExists("CS313")).thenReturn(true);
        when(repository.prerequisites("CS313")).thenReturn(List.of("CS201"));
        doThrow(new RuntimeException("SMTP down"))
                .when(emailSender).send(anyString(), anyString(), anyString());

        String result = service.register("S1", "CS313");

        assertEquals("OK", result);
        verify(repository).saveRegistration("S1", "CS313");
    }

    // A6: assertThrows (аргумент бүрийн null тусад нь)
    @Test
    @DisplayName("A6: null оролтод IllegalArgumentException, хамаарлуудтай харьцаагүй")
    void register_nullArguments_throwsIllegalArgumentAndNeverTouchesDependencies() {
        assertThrows(IllegalArgumentException.class, () -> service.register(null, "CS313"));
        assertThrows(IllegalArgumentException.class, () -> service.register("S1", null));

        verifyNoInteractions(repository, emailSender);
    }

    // A7: ArgumentCaptor
    @Test
    @DisplayName("A7: илгээгдсэн и-мэйлийн биед хичээлийн код орсон")
    void register_success_emailBodyContainsCourseCode() {
        when(repository.findStudent("S1")).thenReturn(activeStudent(List.of("CS201")));
        when(repository.courseExists("CS313")).thenReturn(true);
        when(repository.prerequisites("CS313")).thenReturn(List.of("CS201"));
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        service.register("S1", "CS313");

        verify(emailSender).send(anyString(), anyString(), bodyCaptor.capture());
        assertTrue(bodyCaptor.getValue().contains("CS313"));
    }

    // A8: @ParameterizedTest + @CsvSource (мөр бүр өөр stub тул lenient)
    @ParameterizedTest
    @CsvSource({
        "MISSING,   ERROR_NO_STUDENT",
        "INACTIVE,  ERROR_INACTIVE_STUDENT",
        "NO_COURSE, ERROR_NO_COURSE"
    })
    @DisplayName("A8: алдааны гурван анги нэг параметржүүлсэн тестээр")
    void register_errorClasses_returnExpectedCodeAndDoNotSave(String scenario, String expected) {
        Student student = scenario.equals("MISSING") ? null
                : new Student("S1", "s1@must.edu.mn", !scenario.equals("INACTIVE"), List.of());
        lenient().when(repository.findStudent("S1")).thenReturn(student);
        lenient().when(repository.courseExists("CS313")).thenReturn(!scenario.equals("NO_COURSE"));

        String result = service.register("S1", "CS313");

        assertEquals(expected, result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
    }
}
