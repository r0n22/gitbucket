package app

import util.{WikiUtil, JGitUtil}
import jp.sf.amateras.scalatra.forms._
import org.eclipse.jgit.api.Git

class WikiController extends ControllerBase {

  case class WikiPageEditForm(pageName: String, content: String, message: Option[String], currentPageName: String)
  
  val newForm = mapping(
    "pageName"        -> trim(label("Page name"          , text(required, maxlength(40), pageName, unique))), 
    "content"         -> trim(label("Content"            , text(required))),
    "message"         -> trim(label("Message"            , optional(text()))),
    "currentPageName" -> trim(label("Current page name"  , text()))
  )(WikiPageEditForm.apply)
  
  val editForm = mapping(
    "pageName"        -> trim(label("Page name"          , text(required, maxlength(40), pageName))), 
    "content"         -> trim(label("Content"            , text(required))),
    "message"         -> trim(label("Message"            , optional(text()))),
    "currentPageName" -> trim(label("Current page name"  , text()))
  )(WikiPageEditForm.apply)
  
  get("/:owner/:repository/wiki"){
    val owner      = params("owner")
    val repository = params("repository")
    
    wiki.html.wiki("Home", 
        WikiUtil.getPage(owner, repository, "Home"), 
        JGitUtil.getRepositoryInfo(owner, repository, servletContext))
  }
  
  get("/:owner/:repository/wiki/:page"){
    val owner      = params("owner")
    val repository = params("repository")
    val page       = params("page")
    
    wiki.html.wiki(page, 
        WikiUtil.getPage(owner, repository, page), 
        JGitUtil.getRepositoryInfo(owner, repository, servletContext))
  }
  
  get("/:owner/:repository/wiki/:page/_history"){
    val owner      = params("owner")
    val repository = params("repository")
    val page       = params("page")
    val git        = Git.open(WikiUtil.getWikiRepositoryDir(owner, repository))
    
    wiki.html.wikihistory(Some(page),
      JGitUtil.getCommitLog(git, "master", path = page + ".md")._1,
      JGitUtil.getRepositoryInfo(owner, repository, servletContext))
  }
  
  get("/:owner/:repository/wiki/:page/_compare/:commitId"){
    val owner      = params("owner")
    val repository = params("repository")
    val page       = params("page")
    val commitId   = params("commitId").split("\\.\\.\\.")
    
    println(commitId(0))
    println(commitId(1))
    
    wiki.html.wikicompare(Some(page),
      WikiUtil.getDiffs(Git.open(WikiUtil.getWikiRepositoryDir(owner, repository)), commitId(0), commitId(1)),
      JGitUtil.getRepositoryInfo(owner, repository, servletContext))
  }
  
  get("/:owner/:repository/wiki/_compare/:commitId"){
    val owner      = params("owner")
    val repository = params("repository")
    val commitId   = params("commitId").split("\\.\\.\\.")
    
    println(commitId(0))
    println(commitId(1))
    
    wiki.html.wikicompare(None,
      WikiUtil.getDiffs(Git.open(WikiUtil.getWikiRepositoryDir(owner, repository)), commitId(0), commitId(1)),
      JGitUtil.getRepositoryInfo(owner, repository, servletContext))
  }
  
  get("/:owner/:repository/wiki/:page/_edit"){
    val owner      = params("owner")
    val repository = params("repository")
    val page       = params("page")
    
    wiki.html.wikiedit(page, 
        WikiUtil.getPage(owner, repository, page), 
        JGitUtil.getRepositoryInfo(owner, repository, servletContext))
  }
  
  post("/:owner/:repository/wiki/_edit", editForm){ form =>
    val owner      = params("owner")
    val repository = params("repository")
    
    WikiUtil.savePage(owner, repository, form.currentPageName, form.pageName, 
        form.content, context.loginUser, form.message.getOrElse(""))
    
    redirect("%s/%s/wiki/%s".format(owner, repository, form.pageName))
  }
  
  get("/:owner/:repository/wiki/_new"){
    val owner      = params("owner")
    val repository = params("repository")
    
    wiki.html.wikiedit("", None, 
        JGitUtil.getRepositoryInfo(owner, repository, servletContext))
  }
  
  post("/:owner/:repository/wiki/_new", newForm){ form =>
    val owner      = params("owner")
    val repository = params("repository")
    
    WikiUtil.savePage(owner, repository, form.currentPageName, form.pageName, 
        form.content, context.loginUser, form.message.getOrElse(""))
    
    redirect("%s/%s/wiki/%s".format(owner, repository, form.pageName))
  }
  
  get("/:owner/:repository/wiki/_pages"){
    val owner      = params("owner")
    val repository = params("repository")
    
    wiki.html.wikipages(WikiUtil.getPageList(owner, repository), 
        JGitUtil.getRepositoryInfo(owner, repository, servletContext))
  }
  
  get("/:owner/:repository/wiki/_history"){
    val owner      = params("owner")
    val repository = params("repository")
    
    wiki.html.wikihistory(None,
        JGitUtil.getCommitLog(Git.open(WikiUtil.getWikiRepositoryDir(owner, repository)), "master")._1, 
        JGitUtil.getRepositoryInfo(owner, repository, servletContext))
  }
  
  /**
   * Constraint for the wiki page name.
   */
  def pageName: Constraint = new Constraint(){
    def validate(name: String, value: String): Option[String] = {
      if(!value.matches("^[a-zA-Z0-9\\-_]+$")){
        Some("Page name contains invalid character.")
      } else {
        None
      }
    }
  }
  
  def unique: Constraint = new Constraint(){
    def validate(name: String, value: String): Option[String] = {
      if(WikiUtil.getPageList(params("owner"), params("repository")).contains(value)){
        Some("Page already exists.")
      } else {
        None
      }
    }
  }

}