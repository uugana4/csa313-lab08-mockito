# Лаб 8: Mock объектууд, Mockito (F.CSA313)

**Оюутан:** А.Ууганбаяр
**Оюутны код:** B242270014

## Орчин

```text
openjdk version "17.0.21.1" 2026-11-17
OpenJDK Runtime Environment Homebrew (build 17.0.21.1+0)
OpenJDK 64-Bit Server VM Homebrew (build 17.0.21.1+0, mixed mode, sharing)

Apache Maven 3.10.2 (61b8bc355f4c4325463c22aa9afe4e7811a290f8)
Maven home: /opt/homebrew/Cellar/maven/3.10.2/libexec
Java version: 17.0.21.1, vendor: Homebrew, runtime: /opt/homebrew/Cellar/openjdk@17/17.0.21.1/libexec/openjdk.jdk/Contents/Home
Default locale: en_MN, platform encoding: UTF-8, time zone: Asia/Ulaanbaatar
OS name: "mac os x", version: "26.5.2", arch: "aarch64", family: "mac"
```

## Төсөл

Тестлэгдэх класс: `CourseRegistrationService` (багшийн starter). Хамаарлууд болох `StudentRepository` (өгөгдлийн сан) ба `EmailSender` (и-мэйл) нь интерфейс, бодит хэрэгжүүлэлтгүй. Тестэд тэдгээрийг Mockito-оор орлуулсан.

| Файл | Зорилго |
|---|---|
| `pom.xml` | Maven (JUnit 5.10.2, mockito-junit-jupiter 5.11.0, surefire 3.2.5) |
| `src/main/java/mn/edu/must/sqat/` | Starter код (`CourseRegistrationService`, `Student`, `StudentRepository`, `EmailSender`) |
| `src/test/java/mn/edu/must/sqat/CourseRegistrationServiceTest.java` | Миний бичсэн тест (A1-A8) |
| `results/mvn-test.txt` | Эх кодтой `mvn test` гаралт |
| `results/mvn-test-mutant-1.txt`, `-2.txt`, `-3.txt` | Мутантуудтай `mvn test` гаралт |
| `results/env.txt` | `java -version`, `mvn -version` |

## Хэсэг A: тестүүд

Тестийн тоо: **10** (7 энгийн тест + A8-ийн 3 мөр, мөр бүр тусдаа тестээр тоологдоно). `results/mvn-test.txt`-ийн сүүлийн мөр: `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`.

| № | Тестийн метод | Хэрэгсэл |
|---|---|---|
| A1 | `register_activeStudent_returnsOkSavesAndSendsEmail` | `when` + `verify` |
| A2 | `register_unknownStudent_returnsNoStudentAndDoesNotSaveOrEmail` | `verify(..., never())` |
| A3 | `register_inactiveStudent_returnsInactiveAndNeverTouchesEmail` | `verifyNoInteractions` |
| A4 | `register_missingPrerequisite_returnsPrerequisitesAndDoesNotSave` | `when` + `never` |
| A5 | `register_emailSenderThrows_stillReturnsOkAndRegistrationIsSaved` | `doThrow(...).when(...)` |
| A6 | `register_nullArguments_throwsIllegalArgumentAndNeverTouchesDependencies` | `assertThrows` |
| A7 | `register_success_emailBodyContainsCourseCode` | `ArgumentCaptor` |
| A8 | `register_errorClasses_returnExpectedCodeAndDoNotSave` (3 мөр) | `@ParameterizedTest` + `@CsvSource` |

Нэг дуудалтад matcher хэрэглэсэн бол бүх аргументад matcher хэрэглэсэн (жишээ нь `eq(...)`, `anyString()`, `contains(...)`).

**A8-ийн `lenient()`:** `MockitoExtension` strict stubs горимд ажилладаг тул тест бүрт stub хийсэн дуудалт заавал ашиглагдах ёстой, үгүй бол `UnnecessaryStubbingException` өгдөг. A8-д мөр бүр өөр сценари (оюутан байхгүй, идэвхгүй, хичээл байхгүй) тул нэг мөрөнд зарим stub (жишээ нь `MISSING` мөрөнд `courseExists`) ашиглагдахгүй. Тиймээс stub-уудыг `lenient().when(...)`-оор бичсэн.

## Хэсэг B: мини мутаци

Мутант бүрийн дараа эх кодыг `git checkout`-оор сэргээсэн. Төгсгөлд `mvn test` дахин ногоон (`Tests run: 10, Failures: 0`).

| Мутант | Өөрчлөлт | Файл | Failures / Errors | Унасан тестүүд | Унасан шалтгаан |
|---|---|---|---|---|---|
| 1 | Идэвхгүй оюутны шалгалт `if (!student.active()) return ...`-г комментолсон | `results/mvn-test-mutant-1.txt` | 2 / 0, BUILD FAILURE | A3 `register_inactiveStudent_...`; A8-ийн `[2]` (INACTIVE) мөр | A3: `expected: <ERROR_INACTIVE_STUDENT> but was: <ERROR_NO_COURSE>`; A8: `expected: <ERROR_INACTIVE_STUDENT> but was: <OK>` |
| 2 | `repository.saveRegistration(studentId, courseId);` дуудалтыг комментолсон | `results/mvn-test-mutant-2.txt` | 2 / 0, BUILD FAILURE | A1 `register_activeStudent_...` (:59); A5 `register_emailSenderThrows_...` (:116) | `verify(repository).saveRegistration(...)` шалгалт унасан, учир нь хадгалалт дуудагдаагүй |
| 3 | `if (studentId == null \|\| courseId == null)`-аас `\|\| courseId == null`-г устгасан | `results/mvn-test-mutant-3.txt` | 1 / 0, BUILD FAILURE | A6 `register_nullArguments_...` (:124) | `Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown` |

Гурван мутант бүгд баригдсан, `UnnecessaryStubbingException` (Errors) гараагүй. A6-г эхнээс нь аргумент бүрийн `null`-ийг тусад нь шалгадаг (`register(null, "CS313")` ба `register("S1", null)`) байдлаар бичсэн тул мутант 3-ыг анхнаасаа барьсан, тиймээс мутант амьд үлдсэн хувилбар надад байхгүй.

## Дүгнэлт

Mock нь тестийн явцад Mockito-оор үүсгэгдсэн, тест бүрт хариу нь зааж өгөгдөж (`when/thenReturn`), дуудагдсан эсэхийг нь шалгаж (`verify`) болдог объект. Fake нь гараар бичсэн, жинхэнэ шиг ажилладаг хялбар хэрэгжүүлэлт (жишээ нь санах ойд хадгалдаг `StudentRepository`) бөгөөд төлөвөөр (state) шалгахад тохиромжтой. Mock нь "ямар дуудалт болсон" гэдгийг шалгадаг, fake нь "эцсийн төлөв юу болсон" гэдгийг шалгадаг гэсэн ялгаатай. Mock хийх ёсгүй зүйл бол тестлэгдэж буй классыг өөрийг нь, мөн `Student` мэтийн энгийн өгөгдлийн объектыг. Би `Student`-ийг mock хийлгүй шууд `new Student(...)`-ээр үүсгэсэн, учир нь тэр нь зан төлөвгүй өгөгдөл юм. Харин өгөгдлийн сан, и-мэйл гэх мэт удаан эсвэл гаднах хамаарлыг mock хийх нь зөв. `verify`-г хэтрүүлбэл тест нь кодын дотоод хэрэгжүүлэлтэд (ямар дарааллаар, хэдэн удаа дуудсан) наалддаг болж, зан төлөв өөрчлөгдөөгүй рефакторинг хийхэд ч тест унадаг. Тиймээс би зөвхөн ажиглагдах үр дагавар болох `saveRegistration` ба `send`-ийг шалгаж, `courseExists`, `prerequisites` зэрэг дотоод дуудалтыг `verify` хийгээгүй. Мутацийн туршилт гурван мутантыг барьж, тест нь үнэхээр шалгаж буйг батлав.

## Ажиллуулах

```bash
mkdir -p results
mvn test 2>&1 | tee results/mvn-test.txt
```

## AI ашигласан байдал

Тестийн классын ноорог болон командуудыг Claude-ээр бэлтгүүлсэн. `mvn test`, 3 мутацийн туршилтыг би өөрийн компьютер дээр ажиллуулсан бөгөөд README-ийн хүснэгтийн нэр, тоо нь `results/` дахь файлаас авсан бодит үр дүн. Нэмэлт даалгаваруудыг (CI, AI-ийн тесттэй харьцуулалт) хийгээгүй.
