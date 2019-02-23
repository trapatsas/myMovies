/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package myMovies;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.*;
import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import java.text.SimpleDateFormat;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;


public class ImportData {
    EntityManagerFactory emf = Persistence.createEntityManagerFactory("MyMoviesProjectPU");
    EntityManager em = emf.createEntityManager();
    
    private final String key ="6d4d130943a081ef09726df879406a24"; 
    
    public void clearDataBase(){
        EntityTransaction trans =em.getTransaction();
        trans.begin();
        em.createQuery("DELETE FROM Movie").executeUpdate();
        em.createQuery("DELETE FROM FavoriteList").executeUpdate();
        em.createQuery("DELETE FROM Genre").executeUpdate();
        em.createNativeQuery("ALTER TABLE FAVORITE_LIST ALTER COLUMN ID RESTART WITH 1").executeUpdate();
        trans.commit();
    }
    
    
    GenreJpaController genCon = new GenreJpaController(emf);
    
    Genre g1 = new Genre();
    Genre g2 = new Genre();
    Genre g3 = new Genre();
    
    MovieJpaController movCon = new MovieJpaController(emf);
    
    Movie mov = new Movie();
    
    public void tableGenre() throws IOException, Exception {
        JsonObject api = importTables("https://api.themoviedb.org/3/genre/movie/list?api_key=" + key);
        
        JsonArray genData = api.getJsonArray("genres");
        
        for(JsonObject genreData : genData.getValuesAs(JsonObject.class)){
            if(genreData.getInt("id")==28){
                g1.setId(genreData.getInt("id"));
                g1.setName(genreData.getString("name"));
                genCon.create(g1);
            }
            
            if(genreData.getInt("id")==10749){
                g2.setId(genreData.getInt("id"));
                g2.setName(genreData.getString("name"));
                genCon.create(g2);
            }
            
            if(genreData.getInt("id")==878){
                g3.setId(genreData.getInt("id"));
                g3.setName(genreData.getString("name"));
                genCon.create(g3);
            }
        }
    }
    
    
    public void tableMovies() throws IOException, Exception {
        JsonObject api = importTables("https://api.themoviedb.org/3/discover/movie?"
                + "with_genres=28,878,10749&primary_release_date.gte=2000-01-01&api_key=" + key);
        
        JsonArray movData = api.getJsonArray("results");
        
        for(JsonObject movieData : movData.getValuesAs(JsonObject.class)){
            mov.setId(movieData.getInt("id"));
            
            mov.setTitle(movieData.getString("title"));
            
            SimpleDateFormat dateForm = new SimpleDateFormat("yyyy-MM-dd");
            Date newDate = dateForm.parse(movieData.getString("release_date"));
            mov.setReleaseDate(newDate);
             //TODO
            mov.setRating(movieData.getInt("vote_average"));
            //TODO max 500
            mov.setOverview(movieData.getString("overview").substring(0, Math.min(movieData.getString("overview").length(), 499)));
            
            for(int ids=0;ids<(movieData.getJsonArray("genre_ids").size());ids++){
                if(movieData.getJsonArray("genre_ids").getInt(ids)==g1.getId()){
                    mov.setGenreId(g1);
                    break;
                }
                
                if(movieData.getJsonArray("genre_ids").getInt(ids)==g2.getId()){
                    mov.setGenreId(g2);
                    break;
                }
                
                if(movieData.getJsonArray("genre_ids").getInt(ids)==g3.getId()){
                    mov.setGenreId(g3);
                    break;
                }
            }
            
            movCon.create(mov);
        }
    }
    
    private JsonObject importTables(String urlString) throws IOException, MalformedURLException {
        URL httpAddress = new URL(urlString);
        
        InputStream inp = httpAddress.openStream();
        
        JsonReader reader = Json.createReader(inp);
        
        return reader.readObject();
    }
}
