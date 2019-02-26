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
import javax.swing.JOptionPane;
import javax.swing.JTextArea;

public class ExtractedData {

    EntityManagerFactory emf = Persistence.createEntityManagerFactory("MyMoviesProjectPU");
    EntityManager em = emf.createEntityManager();
    private final String key = "6d4d130943a081ef09726df879406a24";
    private final ArrayList<String> allowedGenres;
    private final ArrayList<Integer> allowedGenreIds;
    GenreJpaController genreController = new GenreJpaController(emf);
    MovieJpaController movieController = new MovieJpaController(emf);
    private JTextArea logArea;

    public ExtractedData(JTextArea jTextArea) {
        this.logArea = jTextArea;

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
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();
        em.createQuery("DELETE FROM Movie").executeUpdate();
        em.createQuery("DELETE FROM FavoriteList").executeUpdate();
        em.createQuery("DELETE FROM Genre").executeUpdate();
        em.createNativeQuery("ALTER TABLE FAVORITE_LIST ALTER COLUMN ID RESTART WITH 1").executeUpdate();
        transaction.commit();
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

        for (int gId : this.allowedGenreIds) {

            int current_page = 1;
            int total_pages;

            JOptionPane.showMessageDialog(null, "Ανάκτηση αποτελεσμάτων από το είδος με κωδικό "
                    + gId, "ΠΛΗΡΟΦΟΡΙΑ", JOptionPane.WARNING_MESSAGE);

            do {
                this.logArea.append("\nΑνάκτηση αποτελεσμάτων από τη σελίδα " + current_page + "... \n");
                JsonObject nextPageResults = discoverMoviesQueryStringBuilder(current_page, gId, key);
                total_pages = nextPageResults.getInt("total_pages");
                this.logArea.append("Ανακτήθηκε η " + current_page + "η σελίδα από σύνολο " + total_pages + " σελίδων. \n");
                JsonArray nextPageMovieResults = nextPageResults.getJsonArray("results");
                saveMovieResults(nextPageMovieResults, logArea);
                current_page++;
            } while (current_page < Math.min(total_pages, 13));

        }
    }

    private void saveMovieResults(JsonArray movieResults, JTextArea logArea) throws Exception {
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

            this.logArea.append("\nΝέα ταινία " + movie.getId() + " τύπου '" + movie.getGenreId().getName() + "' βρέθηκε. \n");

            // Ελέγχουμε αν η ταινία είναι ήδη στη βάση ώστε να αποφύγουμε τις διπλοεγγραφές
            if (movieController.findMovie(movie.getId()) == null) {
                movieController.create(movie);
                this.logArea.append("Η ταινία " + movie.getId() + " αποθηκεύτηκε στη βάση. \n");
            } else {
                this.logArea.append("Η ταινία " + movie.getId() + " υπάρχει ήδη στη βάση. \n");
            }
        }
    }

    private JsonObject importTables(String urlString) throws IOException, MalformedURLException {

        URL url = new URL(urlString);

        return Json.createReader(url.openStream()).readObject();
    }

    private JsonObject discoverMoviesQueryStringBuilder(int page, int genreId, String key) throws IOException {
        return importTables("https://api.themoviedb.org/3/discover/movie?"
                + "with_genres=" + genreId
                + "&primary_release_date.gte=2000-01-01&page=" + page
                + "&api_key=" + key);
    }
}
