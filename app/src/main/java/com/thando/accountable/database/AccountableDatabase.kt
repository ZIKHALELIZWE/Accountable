package com.thando.accountable.database

import android.app.Application
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import com.thando.accountable.database.dataaccessobjects.RepositoryDao
import com.thando.accountable.database.tables.AppSettings
import com.thando.accountable.database.tables.Content
import com.thando.accountable.database.tables.Folder
import com.thando.accountable.database.tables.Goal
import com.thando.accountable.database.tables.GoalTaskDeliverableTime
import com.thando.accountable.database.tables.MarkupLanguage
import com.thando.accountable.database.tables.Script
import com.thando.accountable.database.tables.SpecialCharacters
import com.thando.accountable.database.tables.TeleprompterSettings
import kotlinx.coroutines.coroutineScope

@Database(entities = [
    Folder::class,
    Script::class,
    Goal::class,
    AppSettings::class,
    Content::class,
    MarkupLanguage::class,
    TeleprompterSettings::class,
    SpecialCharacters::class,
    GoalTaskDeliverableTime::class
], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AccountableDatabase: RoomDatabase() {
    abstract val repositoryDao : RepositoryDao

    companion object {
        @Volatile
        private var INSTANCE: AccountableDatabase? = null

        fun closeDatabase(){
            INSTANCE?.close()
            INSTANCE = null
        }

        fun getInstance(application: Application) : AccountableDatabase {
            synchronized(this) {
                if (INSTANCE == null) {
                     INSTANCE = Room.databaseBuilder(
                        application.applicationContext,
                        AccountableDatabase::class.java,
                        "accountable_database"
                    )
                    .addMigrations(
                        Migration(1, 2) { db ->
                            db.execSQL("DROP TABLE IF EXISTS goal_table")
                            db.execSQL("DROP TABLE IF EXISTS times_table")
                            db.execSQL("CREATE TABLE IF NOT EXISTS goal_table (goal_category TEXT NOT NULL, goal_parent INTEGER NOT NULL, goal_colour INTEGER NOT NULL, goal_date_of_completion INTEGER NULL, goal_date_time INTEGER NOT NULL, goal_goal TEXT NOT NULL, goal_location TEXT NOT NULL, goal_num_audios INTEGER NOT NULL, goal_num_documents INTEGER NOT NULL, goal_num_images INTEGER NOT NULL, goal_num_scripts INTEGER NOT NULL, goal_num_videos INTEGER NOT NULL, goal_picture TEXT NULL, goal_position INTEGER NOT NULL, goal_scroll_position INTEGER NOT NULL, goal_size REAL NOT NULL, goal_status TEXT NOT NULL, id INTEGER PRIMARY KEY AUTOINCREMENT NULL)")
                            db.execSQL("CREATE TABLE IF NOT EXISTS times_table (id INTEGER PRIMARY KEY AUTOINCREMENT NULL, times_deliverable INTEGER NOT NULL, times_duration TEXT NOT NULL, times_goal INTEGER NOT NULL," +
                                    " times_start TEXT NOT NULL, times_task INTEGER NOT NULL, times_time_block_type TEXT NOT NULL)")
                        //    db.execSQL("ALTER TABLE content_table ADD content_file_name TEXT DEFAULT '' NOT NULL")
                        //    db.execSQL("ALTER TABLE app_settings_table ADD scripts_order INTEGER DEFAULT 0 NOT NULL")
                        //    db.execSQL("ALTER TABLE folder_table ADD folder_folders_order INTEGER DEFAULT 0 NOT NULL")
                         //   db.execSQL("ALTER TABLE folder_table ADD folder_scripts_order INTEGER DEFAULT 0 NOT NULL")
                            /*db.execSQL("ALTER TABLE app_settings_table DROP COLUMN num_folders")
                    db.execSQL("ALTER TABLE app_settings_table DROP COLUMN num_scripts")
                    db.execSQL("ALTER TABLE app_settings_table DROP COLUMN num_goal_folders")
                    db.execSQL("ALTER TABLE app_settings_table DROP COLUMN num_goals")*/
                        },
                        Migration(2,3) { db ->
                            db.execSQL("ALTER TABLE app_settings_table ADD goal_folders_order INTEGER DEFAULT 1 NOT NULL")
                            db.execSQL("ALTER TABLE app_settings_table ADD goal_scripts_order INTEGER DEFAULT 1 NOT NULL")
                        }
                    )
                     //    .allowMainThreadQueries() // allow for testing only todo
                    .build()
                }
            }
            return INSTANCE!!
        }
    }
}