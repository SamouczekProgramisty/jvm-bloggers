package com.jvm_bloggers.core.social.twitter

import com.jvm_bloggers.core.blogpost_redirect.LinkGenerator
import com.jvm_bloggers.entities.blog.Blog
import com.jvm_bloggers.entities.blog.BlogType
import com.jvm_bloggers.entities.blog_post.BlogPost
import com.jvm_bloggers.entities.newsletter_issue.NewsletterIssue
import com.jvm_bloggers.utils.NowProvider
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import static com.jvm_bloggers.entities.blog.BlogType.COMPANY
import static com.jvm_bloggers.entities.blog.BlogType.PERSONAL

class TweetContentGeneratorSpec extends Specification {

    private static final long ISSUE_NUMBER = 999L
    private static final String LINK = "http://jvm-bloggers.com/issue/$ISSUE_NUMBER"

    private static final Random randomJsonId = new Random()
    private static final NowProvider nowProvider = new NowProvider()

    private final LinkGenerator linkGenerator = Stub(LinkGenerator)

    @Subject
    private TweetContentGenerator contentGenerator = new TweetContentGenerator(this.linkGenerator)

    def setup() {
        linkGenerator.generateIssueLink(_) >> { args -> LINK }
    }

    def "Should generate a Tweet content with an issue number and link"() {
        given:
        NewsletterIssue issue = NewsletterIssue
                .builder()
                .issueNumber(ISSUE_NUMBER)
                .heading("issue heading")
                .blogPosts(posts())
                .build()

        when:
        String tweetContent = contentGenerator.generateTweetContent(issue)

        then:
        tweetContent.contains(issue.issueNumber as String)
        tweetContent.contains(LINK)
    }

    def "Should add two twitter handles of personal blogs"() {
        given:
        NewsletterIssue issue = NewsletterIssue
                .builder()
                .issueNumber(ISSUE_NUMBER)
                .heading("issue heading")
                .blogPosts(posts())
                .build()

        when:
        String tweetContent = contentGenerator.generateTweetContent(issue)

        then:
        def personal = /@personal/
        def personalBlogs = (tweetContent =~ /$personal/)
        assert personalBlogs.count == 2
    }

    def "Should add one twitter handle of company blog"() {
        given:
        NewsletterIssue issue = NewsletterIssue
                .builder()
                .issueNumber(ISSUE_NUMBER)
                .heading("issue heading")
                .blogPosts(posts())
                .build()

        when:
        String tweetContent = contentGenerator.generateTweetContent(issue)

        then:
        def company = /@company/
        def companyBlogs = (tweetContent =~ /$company/)
        assert companyBlogs.count == 1
    }

    def "Should add company twitter handle as the second on handles list"() {
        given:
        NewsletterIssue issue = NewsletterIssue
                .builder()
                .issueNumber(ISSUE_NUMBER)
                .heading("issue heading")
                .blogPosts(posts())
                .build()

        when:
        String tweetContent = contentGenerator.generateTweetContent(issue)

        then:
        def handles = /.*@personal\d{1}, @company\d{1} i @personal\d{1}.*/
        tweetContent ==~ /$handles/
    }

    def "Should add no company handle if there are no company blogs"() {
        given:
        NewsletterIssue issue = NewsletterIssue
                .builder()
                .issueNumber(ISSUE_NUMBER)
                .heading("issue heading")
                .blogPosts(noCompanyPosts())
                .build()

        when:
        String tweetContent = contentGenerator.generateTweetContent(issue)

        then:
        def company = /@company/
        def companyBlogs = (tweetContent =~ /$company/)
        assert companyBlogs.count == 0
    }

    def "Should add one personal and one company handles only if there is no second personal handle"() {
        given:
        NewsletterIssue issue = NewsletterIssue
                .builder()
                .issueNumber(ISSUE_NUMBER)
                .heading("issue heading")
                .blogPosts(singlePersonalPost())
                .build()

        when:
        String tweetContent = contentGenerator.generateTweetContent(issue)

        then:
        def personal = /@personal/
        def personalBlogs = (tweetContent =~ /$personal/)
        assert personalBlogs.count == 1

        and:
        def company = /@company/
        def companyBlogs = (tweetContent =~ /$company/)
        assert companyBlogs.count == 1
    }

    def "Should not add the second personal twitter handle if message is too long"() {
        given:
        NewsletterIssue issue = NewsletterIssue
                .builder()
                .issueNumber(ISSUE_NUMBER)
                .heading("issue heading")
                .blogPosts(postsWithLongHandles())
                .build()

        when:
        String tweetContent = contentGenerator.generateTweetContent(issue)

        then:
        def handles = /.*@veryLongPersonalHandle\d{1} i @veryLongCompanyHandle\d{1}.*/
        tweetContent ==~ /$handles/
    }

    @Unroll
    def "Should always have java and jvm tags at the end"() {
        given:
        NewsletterIssue issue = NewsletterIssue
                .builder()
                .issueNumber(ISSUE_NUMBER)
                .heading("issue heading")
                .blogPosts(posts)
                .build()

        when:
        String tweetContent = contentGenerator.generateTweetContent(issue)

        then:
        tweetContent.endsWith("#java #jvm")

        where:
        posts << [posts(), postsWithLongHandles()]
    }

    private Collection<BlogPost> noCompanyPosts() {
        List<BlogPost> posts = new ArrayList<>()
        posts.add(blogPost(blog("@personal1", PERSONAL)))
        posts.add(blogPost(blog("@personal2", PERSONAL)))
        posts.add(blogPost(blog("@personal3", PERSONAL)))
        return posts
    }

    private Collection<BlogPost> singlePersonalPost() {
        List<BlogPost> posts = new ArrayList<>()
        posts.add(blogPost(blog("@personal1", PERSONAL)))
        posts.add(blogPost(blog("@company1", COMPANY)))
        posts.add(blogPost(blog("@company2", COMPANY)))
        return posts
    }

    private Collection<BlogPost> posts() {
        List<BlogPost> posts = new ArrayList<>()
        posts.add(blogPost(blog("@personal1", PERSONAL)))
        posts.add(blogPost(blog("@personal2", PERSONAL)))
        posts.add(blogPost(blog("@company1", COMPANY)))
        posts.add(blogPost(blog("@company2", COMPANY)))
        posts.add(blogPost(blog("@personal3", PERSONAL)))
        return posts
    }

    private Collection<BlogPost> postsWithLongHandles() {
        List<BlogPost> posts = new ArrayList<>()
        posts.add(blogPost(blog("@veryLongPersonalHandle1", PERSONAL)))
        posts.add(blogPost(blog("@veryLongPersonalHandle2", PERSONAL)))
        posts.add(blogPost(blog("@veryLongCompanyHandle1", COMPANY)))
        posts.add(blogPost(blog("@veryLongCompanyHandle2", COMPANY)))
        posts.add(blogPost(blog("@veryLongPersonalHandle3", PERSONAL)))
        return posts
    }

    private BlogPost blogPost(Blog blog) {
        BlogPost
            .builder()
            .title("title")
            .url("url")
            .publishedDate(nowProvider.now())
            .blog(blog)
            .build()
    }

    private Blog blog(String twitterHandle, BlogType blogType) {
        Blog.builder()
            .bookmarkableId(randomJsonId.nextLong().toString())
            .author("author")
            .twitter(twitterHandle)
            .rss("rss")
            .url("url")
            .dateAdded(nowProvider.now())
            .blogType(blogType)
            .build()
    }

}
