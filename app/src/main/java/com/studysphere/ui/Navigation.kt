package com.studysphere.ui

sealed class Screen(val route: String) {
    object Dashboard      : Screen("dashboard")
    object Attendance     : Screen("attendance")
    object AttendanceDetail : Screen("attendance_detail/{subjectId}") {
        fun createRoute(subjectId: Long) = "attendance_detail/$subjectId"
    }
    object Assignments    : Screen("assignments")
    object Subjects       : Screen("subjects")
}
