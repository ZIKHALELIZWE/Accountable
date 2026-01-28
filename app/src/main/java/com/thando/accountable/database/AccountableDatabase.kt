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
import com.thando.accountable.database.tables.Deliverable
import com.thando.accountable.database.tables.Folder
import com.thando.accountable.database.tables.Goal
import com.thando.accountable.database.tables.GoalTaskDeliverableTime
import com.thando.accountable.database.tables.Marker
import com.thando.accountable.database.tables.MarkupLanguage
import com.thando.accountable.database.tables.Script
import com.thando.accountable.database.tables.SpecialCharacters
import com.thando.accountable.database.tables.Task
import com.thando.accountable.database.tables.TeleprompterSettings
import java.time.LocalDateTime
import java.time.ZoneOffset

@Database(entities = [
    Folder::class,
    Script::class,
    Goal::class,
    AppSettings::class,
    Content::class,
    MarkupLanguage::class,
    TeleprompterSettings::class,
    SpecialCharacters::class,
    GoalTaskDeliverableTime::class,
    Task::class,
    Deliverable::class,
    Marker::class
], version = 10, exportSchema = false)
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
                        },
                        Migration(2,3) { db ->
                            db.execSQL("ALTER TABLE app_settings_table ADD goal_folders_order INTEGER DEFAULT 1 NOT NULL")
                            db.execSQL("ALTER TABLE app_settings_table ADD goal_scripts_order INTEGER DEFAULT 1 NOT NULL")
                        },
                        Migration(3,4) { db ->
                            db.execSQL("ALTER TABLE goal_table ADD goal_selected_tab TEXT DEFAULT 'TASKS' NOT NULL")
                        },
                        Migration(4,5){ db ->
                            db.execSQL("ALTER TABLE goal_table ADD goal_tab_list_state INTEGER DEFAULT 0 NOT NULL")
                        },
                        Migration(5,6){ db ->
                            db.execSQL("CREATE TABLE IF NOT EXISTS marker_table (id INTEGER PRIMARY KEY AUTOINCREMENT NULL, marker_parent INTEGER NOT NULL, marker_position INTEGER NOT NULL, marker_date_time INTEGER NOT NULL," +
                                    " marker_edit_scroll_position INTEGER NOT NULL, marker_marker TEXT NOT NULL)")
                            db.execSQL("CREATE TABLE IF NOT EXISTS deliverable_table (id INTEGER PRIMARY KEY AUTOINCREMENT NULL, deliverable_parent INTEGER NOT NULL, deliverable_position INTEGER NOT NULL, deliverable_initial_date INTEGER NOT NULL," +
                                    " deliverable_end_date INTEGER NOT NULL, deliverable_end_type TEXT NOT NULL, deliverable_edit_scroll_position INTEGER NOT NULL, deliverable_deliverable TEXT NOT NULL, deliverable_status TEXT NOT NULL, " +
                                    "deliverable_location TEXT NOT NULL, deliverable_size FLOAT NOT NULL, deliverable_num_images INTEGER NOT NULL, deliverable_num_videos INTEGER NOT NULL, deliverable_num_audios INTEGER NOT NULL, " +
                                    "deliverable_num_documents INTEGER NOT NULL, deliverable_num_scripts INTEGER NOT NULL)")
                            db.execSQL("CREATE TABLE IF NOT EXISTS task_table (id INTEGER PRIMARY KEY AUTOINCREMENT NULL, task_parent INTEGER NOT NULL, task_parent_type TEXT NOT NULL, task_position INTEGER NOT NULL," +
                                    " task_initial_date INTEGER NOT NULL, task_end_date INTEGER NOT NULL, task_end_type TEXT NOT NULL, task_edit_scroll_position INTEGER NOT NULL, task_task TEXT NOT NULL, task_status TEXT NOT NULL, " +
                                    "task_colour INTEGER NOT NULL, task_location TEXT NOT NULL, task_size FLOAT NOT NULL, task_num_images INTEGER NOT NULL, task_num_videos INTEGER NOT NULL, task_num_audios INTEGER NOT NULL, " +
                                    "task_num_documents INTEGER NOT NULL, task_num_scripts INTEGER NOT NULL)")

                            // 1. Create new table with updated default
                             db.execSQL("""
                             CREATE TABLE goal_table_new (
                             id INTEGER PRIMARY KEY AUTOINCREMENT NULL,
                             goal_category TEXT NOT NULL,
                             goal_colour INTEGER NOT NULL,
                             goal_date_of_completion INTEGER NOT NULL,
                             goal_date_time INTEGER NOT NULL,
                             goal_goal TEXT NOT NULL,
                             goal_location TEXT NOT NULL,
                             goal_num_audios INTEGER NOT NULL,
                             goal_num_documents INTEGER NOT NULL,
                             goal_num_images INTEGER NOT NULL,
                             goal_num_scripts INTEGER NOT NULL,
                             goal_num_videos INTEGER NOT NULL,
                             goal_parent INTEGER NOT NULL,
                             goal_picture TEXT NULL,
                             goal_position INTEGER NOT NULL,
                             goal_scroll_position INTEGER NOT NULL,
                             goal_selected_tab TEXT NOT NULL,
                             goal_size REAL NOT NULL,
                             goal_status TEXT NOT NULL,
                             goal_tab_list_state INTEGER NOT NULL
                             )
                             """.trimIndent())
                             // 2. Drop old table
                             db.execSQL("DROP TABLE goal_table")
                             // 3. Rename new table
                             db.execSQL("ALTER TABLE goal_table_new RENAME TO goal_table")

                             // 1. Create new table with updated default
                              db.execSQL("""
                              CREATE TABLE times_table_new (
                              id INTEGER PRIMARY KEY AUTOINCREMENT NULL,
                              times_deliverable INTEGER NOT NULL,
                              times_duration INTEGER NOT NULL,
                              times_goal INTEGER NOT NULL,
                              times_start INTEGER NOT NULL,
                              times_task INTEGER NOT NULL,
                              times_time_block_type TEXT NOT NULL
                              )
                              """.trimIndent())
                              // 2. Drop old table
                              db.execSQL("DROP TABLE times_table")
                              // 3. Rename new table
                              db.execSQL("ALTER TABLE times_table_new RENAME TO times_table")
                        },
                        Migration(6,7){ db ->
                            db.execSQL("ALTER TABLE times_table DROP COLUMN times_goal")
                            db.execSQL("ALTER TABLE times_table DROP COLUMN times_task")
                            db.execSQL("ALTER TABLE times_table DROP COLUMN times_deliverable")
                            db.execSQL("ALTER TABLE times_table ADD times_parent INTEGER NOT NULL")
                            db.execSQL("ALTER TABLE times_table ADD times_type TEXT NOT NULL")
                        },
                        Migration(7,8){ db ->
                            db.execSQL("ALTER TABLE task_table ADD task_type TEXT DEFAULT NORMAL NOT NULL")
                            db.execSQL("ALTER TABLE task_table ADD task_quantity INTEGER DEFAULT 0 NOT NULL")
                            db.execSQL("ALTER TABLE task_table ADD task_time INTEGER DEFAULT undefined NOT NULL")
                        },
                        Migration(8,9){ db ->
                            db.execSQL("ALTER TABLE goal_table ADD goal_end_date INTEGER DEFAULT ${LocalDateTime.now().toInstant(ZoneOffset.UTC)?.toEpochMilli()} NOT NULL")
                            db.execSQL("ALTER TABLE goal_table ADD goal_end_type TEXT DEFAULT ${Goal.GoalEndType.UNDEFINED.name} NOT NULL")
                        },
                        Migration(9,10) { db ->
                            db.execSQL("ALTER TABLE deliverable_table ADD deliverable_goal_id INTEGER DEFAULT ${null} NULL")
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