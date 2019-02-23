/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package myMovies;

import java.io.Serializable;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import myMovies.exceptions.NonexistentEntityException;
import myMovies.exceptions.PreexistingEntityException;

/**
 *
 * @author trapa
 */
public class MovieJpaController implements Serializable {

    public MovieJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Movie movie) throws PreexistingEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            FavoriteList favoriteListId = movie.getFavoriteListId();
            if (favoriteListId != null) {
                favoriteListId = em.getReference(favoriteListId.getClass(), favoriteListId.getId());
                movie.setFavoriteListId(favoriteListId);
            }
            Genre genreId = movie.getGenreId();
            if (genreId != null) {
                genreId = em.getReference(genreId.getClass(), genreId.getId());
                movie.setGenreId(genreId);
            }
            em.persist(movie);
            if (favoriteListId != null) {
                favoriteListId.getMovieCollection().add(movie);
                favoriteListId = em.merge(favoriteListId);
            }
            if (genreId != null) {
                genreId.getMovieCollection().add(movie);
                genreId = em.merge(genreId);
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            if (findMovie(movie.getId()) != null) {
                throw new PreexistingEntityException("Movie " + movie + " already exists.", ex);
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Movie movie) throws NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Movie persistentMovie = em.find(Movie.class, movie.getId());
            FavoriteList favoriteListIdOld = persistentMovie.getFavoriteListId();
            FavoriteList favoriteListIdNew = movie.getFavoriteListId();
            Genre genreIdOld = persistentMovie.getGenreId();
            Genre genreIdNew = movie.getGenreId();
            if (favoriteListIdNew != null) {
                favoriteListIdNew = em.getReference(favoriteListIdNew.getClass(), favoriteListIdNew.getId());
                movie.setFavoriteListId(favoriteListIdNew);
            }
            if (genreIdNew != null) {
                genreIdNew = em.getReference(genreIdNew.getClass(), genreIdNew.getId());
                movie.setGenreId(genreIdNew);
            }
            movie = em.merge(movie);
            if (favoriteListIdOld != null && !favoriteListIdOld.equals(favoriteListIdNew)) {
                favoriteListIdOld.getMovieCollection().remove(movie);
                favoriteListIdOld = em.merge(favoriteListIdOld);
            }
            if (favoriteListIdNew != null && !favoriteListIdNew.equals(favoriteListIdOld)) {
                favoriteListIdNew.getMovieCollection().add(movie);
                favoriteListIdNew = em.merge(favoriteListIdNew);
            }
            if (genreIdOld != null && !genreIdOld.equals(genreIdNew)) {
                genreIdOld.getMovieCollection().remove(movie);
                genreIdOld = em.merge(genreIdOld);
            }
            if (genreIdNew != null && !genreIdNew.equals(genreIdOld)) {
                genreIdNew.getMovieCollection().add(movie);
                genreIdNew = em.merge(genreIdNew);
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Integer id = movie.getId();
                if (findMovie(id) == null) {
                    throw new NonexistentEntityException("The movie with id " + id + " no longer exists.");
                }
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void destroy(Integer id) throws NonexistentEntityException {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Movie movie;
            try {
                movie = em.getReference(Movie.class, id);
                movie.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The movie with id " + id + " no longer exists.", enfe);
            }
            FavoriteList favoriteListId = movie.getFavoriteListId();
            if (favoriteListId != null) {
                favoriteListId.getMovieCollection().remove(movie);
                favoriteListId = em.merge(favoriteListId);
            }
            Genre genreId = movie.getGenreId();
            if (genreId != null) {
                genreId.getMovieCollection().remove(movie);
                genreId = em.merge(genreId);
            }
            em.remove(movie);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Movie> findMovieEntities() {
        return findMovieEntities(true, -1, -1);
    }

    public List<Movie> findMovieEntities(int maxResults, int firstResult) {
        return findMovieEntities(false, maxResults, firstResult);
    }

    private List<Movie> findMovieEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Movie.class));
            Query q = em.createQuery(cq);
            if (!all) {
                q.setMaxResults(maxResults);
                q.setFirstResult(firstResult);
            }
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public Movie findMovie(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Movie.class, id);
        } finally {
            em.close();
        }
    }

    public int getMovieCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Movie> rt = cq.from(Movie.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}
