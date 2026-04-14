package com.studysphere.data.db

import androidx.room.TypeConverter
import com.studysphere.data.models.AttendanceStatus
import com.studysphere.data.models.AssignmentStatus
import com.studysphere.data.models.Priority

class Converters {
    @TypeConverter fun fromAttendanceStatus(v: AttendanceStatus): String = v.name
    @TypeConverter fun toAttendanceStatus(v: String): AttendanceStatus = AttendanceStatus.valueOf(v)

    @TypeConverter fun fromAssignmentStatus(v: AssignmentStatus): String = v.name
    @TypeConverter fun toAssignmentStatus(v: String): AssignmentStatus = AssignmentStatus.valueOf(v)

    @TypeConverter fun fromPriority(v: Priority): String = v.name
    @TypeConverter fun toPriority(v: String): Priority = Priority.valueOf(v)
}
