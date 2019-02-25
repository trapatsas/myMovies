package myMovies;

import java.io.IOException;
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

public class ExtractedData {

    EntityManagerFactory emf = Persistence.createEntityManagerFactory("MyMoviesProjectPU");
    EntityManager em = emf.createEntityManager();
    private final String key = "6d4d130943a081ef09726df879406a24";
    private final ArrayList<String> allowedGenres;
    private final ArrayList<Integer> allowedGenreIds;
    GenreJpaController genreController = new GenreJpaController(emf);
    MovieJpaController movieController = new MovieJpaController(emf);

    public ExtractedData() {
        this.allowedGenres = new ArrayList<>();
        allowedGenres.add("Action");
        allowedGenres.add("Romance");
        allowedGenres.add("Science Fiction");

        this.allowedGenreIds = new ArrayList<>();
        allowedGenreIds.add(28);
        allowedGenreIds.add(878);
        allowedGenreIds.add(10749);
    }

    public void deleteDataBase() {
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        em.createQuery("DELETE FROM Movie").executeUpdate();
        em.createQuery("DELETE FROM FavoriteList").executeUpdate();
        em.createQuery("DELETE FROM Genre").executeUpdate();
        em.createNativeQuery("ALTER TABLE FAVORITE_LIST ALTER COLUMN ID RESTART WITH 1").executeUpdate();
        trans.commit();
    }

    public void fillGenreTable() throws IOException, Exception {
        JsonObject api = importTables("https://api.themoviedb.org/3/genre/movie/list?api_key=" + key);

        JsonArray genreData = api.getJsonArray("genres");

        for (JsonObject genre : genreData.getValuesAs(JsonObject.class)) {
            if (allowedGenres.contains(genre.getString("name"))) {
                genreController.create(new Genre(genre.getInt("id"), genre.getString("name")));
            }
        }
    }

    public void fillMovieTable() throws IOException, Exception {
        JsonObject api = importTables("https://api.themoviedb.org/3/discover/movie?"
                + "with_genres=28,878,10749&primary_release_date.gte=2000-01-01&api_key=" + key);

        JsonArray movieResults = api.getJsonArray("results");

        for (JsonObject movieData : movieResults.getValuesAs(JsonObject.class)) {

            SimpleDateFormat dateForm = new SimpleDateFormat("yyyy-MM-dd");

            Movie movie = new Movie(
                    movieData.getInt("id"),
                    movieData.getString("title"),
                    dateForm.parse(movieData.getString("release_date")),
                    movieData.getJsonNumber("vote_average").bigDecimalValue().floatValue(),
                    movieData.getString("overview").substring(0,
                            Math.min(movieData.getString("overview").length(), 499))
            );

            JsonArray movieGenres = movieData.getJsonArray("genre_ids");

            ArrayList<Integer> movieGenreIds = new ArrayList<>();
            if (movieGenres != null) {
                for (int i = 0; i < movieGenres.size(); i++) {
                    movieGenreIds.add(movieGenres.getInt(i));
                }
            }

            for (int genreId : this.allowedGenreIds) {
                if (movieGenreIds.contains(genreId)) {
                    movie.setGenreId(genreController.findGenre(genreId));
                    break;
                }
            }

            movieController.create(movie);
        }
    }

    private JsonObject importTables(String urlString) throws IOException, MalformedURLException {

        URL url = new URL(urlString);

        return Json.createReader(url.openStream()).readObject();
    }
}
