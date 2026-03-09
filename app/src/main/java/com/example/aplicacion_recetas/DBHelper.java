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
    private static final int DATABASE_VERSION = 3; // se incrementa cuando se hacen cambios en la estructura
    public static final String TABLE_RECETAS = "recetas";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITULO = "titulo";
    public static final String COLUMN_CATEGORIA = "categoria";
    public static final String COLUMN_TIEMPO = "tiempo";
    public static final String COLUMN_INGREDIENTES = "ingredientes";
    public static final String COLUMN_PASOS = "pasos";
    public static final String COLUMN_FOTO = "foto";
    public static final String COLUMN_FAVORITA = "favorita";

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
                + COLUMN_PASOS + " TEXT, "
                + COLUMN_FOTO + " TEXT, "
                + COLUMN_FAVORITA + " INTEGER DEFAULT 0"
                + ");";
        db.execSQL(CREATE_TABLE);
    }

    // Se llama cuando se incrementa la versión de la db
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // se añade la columna favorita a la tabla existente
        if(oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_RECETAS + " ADD COLUMN " + COLUMN_FAVORITA + " INTEGER DEFAULT 0");
        }
    }

    // Añadir una receta a la db, devuelve el id automático de la nueva fila
    public long agregarReceta(Receta receta) {
        SQLiteDatabase db = this.getWritableDatabase();
        // ContenValues para mapear las columnas
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITULO, receta.titulo);
        values.put(COLUMN_CATEGORIA, receta.categoria);
        values.put(COLUMN_TIEMPO, receta.tiempo);
        values.put(COLUMN_INGREDIENTES, receta.ingredientes);
        values.put(COLUMN_PASOS, receta.pasos);
        values.put(COLUMN_FOTO, receta.fotoUri);
        values.put(COLUMN_FAVORITA, receta.favorita ? 1 :0);
        long id = db.insert(TABLE_RECETAS, null, values);
        db.close();
        return id;
    }

    // Elimina una receta de la db usando su id
    public void eliminarReceta(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_RECETAS, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    // Obtiene todas las recetas de la db, convierte cada fila en un onjeto Receta
    public List<Receta> getRecetas() {
        List<Receta> lista = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_RECETAS, null);
        // recorrer fila por fila con el cursor
        while (cursor.moveToNext()) {
            Receta r = new Receta();
            // asigna cada columna a los atributos del objeto
            r.id = cursor.getInt(0);
            r.titulo = cursor.getString(1);
            r.categoria = cursor.getString(2);
            r.tiempo = cursor.getInt(3);
            r.ingredientes = cursor.getString(4);
            r.pasos = cursor.getString(5);
            r.fotoUri = cursor.getString(6);
            r.favorita = cursor.getInt(7) == 1; // convierte de int a boolean
            lista.add(r);
        }
        cursor.close();
        db.close();
        return lista;
    }

    // Obtiene solo las recetas favoritas
    public List<Receta> getRecetasFavoritas() {
        List<Receta> lista = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_RECETAS + " WHERE " + COLUMN_FAVORITA + "=1", null);
        while (cursor.moveToNext()) {
            Receta r = new Receta();
            r.id = cursor.getInt(0);
            r.titulo = cursor.getString(1);
            r.categoria = cursor.getString(2);
            r.tiempo = cursor.getInt(3);
            r.ingredientes = cursor.getString(4);
            r.pasos = cursor.getString(5);
            r.fotoUri = cursor.getString(6);
            r.favorita = cursor.getInt(7) == 1;
            lista.add(r);
        }
        cursor.close();
        db.close();
        return lista;
    }

    // Actualiza el estado "favorita" de una receta
    public void updateFavorita(int id, boolean favorita) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_FAVORITA, favorita ? 1 : 0);
        db.update(TABLE_RECETAS, values, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }
}
