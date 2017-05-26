package streama

import grails.converters.JSON

import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class TvShowController {

  def theMovieDbService
  def videoService

  static responseFormats = ['json', 'xml']
  static allowedMethods = [save: "POST", delete: "DELETE"]

  def index() {
    JSON.use('fullShow') {
      response.setStatus(OK.value())
      render (TvShow.findAllByDeletedNotEqual(true) as JSON)
    }
  }

  @Transactional
  def save() {
    def data = request.JSON

    if (data == null) {
      render status: NOT_FOUND
      return
    }

    TvShow tvShow = TvShow.findByApiId(data.apiId)

    if (tvShow == null) {
      tvShow = new TvShow()
    }
    tvShow.properties = data
    tvShow.deleted = false

    if(!tvShow.imdb_id && !data.manualInput){
      tvShow.imdb_id = tvShow.externalLinks?.imdb_id
    }

    tvShow.validate()
    if (tvShow.hasErrors()) {
      render status: NOT_ACCEPTABLE
      return
    }

    tvShow.save flush: true
    respond tvShow, [status: CREATED]
  }

  def show(TvShow tvShow) {
    JSON.use('fullShow') {
      respond tvShow, [status: OK]
    }
  }

  def episodesForTvShow(TvShow tvShow) {
    JSON.use('episodesForTvShow') {
      respond Episode.findAllByShowAndDeletedNotEqual(tvShow, true), [status: OK]
    }
  }

  def adminEpisodesForTvShow(TvShow tvShowInstance) {
    JSON.use('adminEpisodesForTvShow') {
      respond Episode.findAllByShowAndDeletedNotEqual(tvShowInstance, true), [status: OK]
    }
  }

  @Transactional
  def delete(TvShow tvShow) {

    if (tvShow == null) {
      render status: NOT_FOUND
      return
    }

    tvShow.deleted = true
    tvShow.save flush: true, failOnError: true

    tvShow.episodes*.deleted = true
    tvShow.episodes*.save flush: true, failOnError: true

    render status: NO_CONTENT
  }

  @Transactional
  def removeSeason() {
    TvShow tvShow = TvShow.get(params.getInt('showId'))
    int season = params.getInt('season_number')

    if (!tvShow || season == null) {
      render status: NOT_FOUND
      return
    }

    def episodes = Episode.findAllByShowAndSeason_numberAndDeletedNotEqual(tvShow, season, true)
    episodes.each{episode ->
      videoService.deleteVideoAndAssociations(episode)
    }

    render status: NO_CONTENT
  }
}