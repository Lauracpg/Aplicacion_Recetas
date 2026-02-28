package com.example.aplicacion_recetas;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "recetas.db";
    private static final int DATABASE_VERSION = 1;
    public static final String TABLE_RECETAS = "recetas";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITULO = "titulo";
    public static final String COLUMN_CATEGORIA = "categoria";
    public static final String COLUMN_TIEMPO = "tiempo";
    public static final String COLUMN_INGREDIENTES = "ingredientes";
    public static final String COLUMN_PASOS = "pasos";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_RECETAS + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_TITULO + " TEXT NOT NULL, "
                + COLUMN_CATEGORIA + " TEXT, "
                + COLUMN_TIEMPO + " INTEGER, "
                + COLUMN_INGREDIENTES + " TEXT, "
                + COLUMN_PASOS + " TEXT"
                + ");";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECETAS);
        onCreate(db);
    }

    // Añadir una receta
    public long agregarReceta(Receta receta) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITULO, receta.titulo);
        values.put(COLUMN_CATEGORIA, receta.categoria);
        values.put(COLUMN_TIEMPO, receta.tiempo);
        values.put(COLUMN_INGREDIENTES, receta.ingredientes);
        values.put(COLUMN_PASOS, receta.pasos);

        long id = db.insert(TABLE_RECETAS, null, values);
        db.close();
        return id;
    }
    public void eliminarReceta(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_RECETAS, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    // Listar recetas
    public List<Receta> getRecetas() {
        List<Receta> lista = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_RECETAS, null);
        if (cursor.moveToFirst()) {
            do {
                Receta r = new Receta();
                r.id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                r.titulo = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITULO));
                r.categoria = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORIA));
                r.tiempo = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TIEMPO));
                r.ingredientes = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INGREDIENTES));
                r.pasos = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASOS));
                lista.add(r);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return lista;
    }
}
