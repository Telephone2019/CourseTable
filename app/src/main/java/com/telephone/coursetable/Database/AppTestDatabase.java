package com.telephone.coursetable.Database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

/**
 * @clear
 */
@Database(entities = {GoToClass.class, ClassInfo.class, TermInfo.class, User.class, PersonInfo.class, GraduationScore.class, Grades.class, ExamInfo.class, CET.class, LAB.class, MyComment.class}, version = 5, exportSchema = false)
public abstract class AppTestDatabase extends RoomDatabase {
    public abstract GoToClassDao goToClassDao();
    public abstract ClassInfoDao classInfoDao();
    public abstract TermInfoDao termInfoDao();
    public abstract UserDao userDao();
    public abstract PersonInfoDao personInfoDao();
    public abstract GraduationScoreDao graduationScoreDao();
    public abstract GradesDao gradesDao();
    public abstract ExamInfoDao examInfoDao();
    public abstract CETDao cetDao();
    public abstract LABDao labDao();
    public abstract MyCommentDao myCommentDao();
}
