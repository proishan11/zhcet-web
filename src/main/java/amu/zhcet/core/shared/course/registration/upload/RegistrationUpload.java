package amu.zhcet.core.shared.course.registration.upload;

import com.j256.simplecsv.common.CsvColumn;
import lombok.Data;

@Data
class RegistrationUpload {
    @CsvColumn(columnName = "faculty_no", mustNotBeBlank = true)
    private String facultyNo;
    @CsvColumn
    private char mode;
}