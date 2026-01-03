package com.emreyildirim.matchhuntv1.data.repository

import android.net.Uri
import android.util.Log
import com.emreyildirim.matchhuntv1.data.model.Comment
import com.emreyildirim.matchhuntv1.data.model.Post
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

class PostRepository {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val postsCollection = db.collection("posts")
    private val commentsCollection = db.collection("comments")
    private val storageRef = storage.reference.child("post_images")
    
    // Sayfa başına gösterilecek post sayısı
    private val pageSize = 10

    suspend fun createPost(
        userId: String,
        userName: String,
        imageUri: Uri,
        description: String,
        sportType: String
    ): Result<Post> {
        return try {
            val postId = UUID.randomUUID().toString()
            val imageRef = storageRef.child("$postId.jpg")
            
            // Upload image to Firebase Storage
            Log.d("PostRepository", "Uploading image to Storage...")
            imageRef.putFile(imageUri).await()
            val imageUrl = imageRef.downloadUrl.await().toString()
            Log.d("PostRepository", "Image uploaded successfully. URL: $imageUrl")

            // Create post object
            val post = Post(
                id = postId,
                userId = userId,
                userName = userName,
                imageUrl = imageUrl,
                description = description,
                sportType = sportType,
                createdAt = Date()
            )

            // Save post to Firestore
            Log.d("PostRepository", "Saving post to Firestore...")
            postsCollection.document(postId).set(post).await()
            Log.d("PostRepository", "Post saved successfully to Firestore")
            
            Result.success(post)
        } catch (e: Exception) {
            Log.e("PostRepository", "Error creating post", e)
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String, userId: String): Result<Unit> {
        return try {
            val postRef = postsCollection.document(postId)
            val post = postRef.get().await().toObject(Post::class.java)
                ?: return Result.failure(Exception("Post bulunamadı"))

            // Kısa kontrol - sadece sahip kontrolü
            if (post.userId != userId) {
                return Result.failure(Exception("Bu postu silme yetkiniz yok"))
            }

            // Görseli sil
            if (post.imageUrl.isNotEmpty()) {
                try {
                    storage.getReferenceFromUrl(post.imageUrl).delete().await()
                } catch (e: Exception) {
                    Log.w("PostRepository", "Image delete failed: ${e.message}")
                }
            }

            // Yorumları sil
            try {
                val comments = commentsCollection.whereEqualTo("postId", postId).get().await()
                val batch = db.batch()
                comments.documents.forEach { batch.delete(it.reference) }
                if (comments.documents.isNotEmpty()) batch.commit().await()
            } catch (e: Exception) {
                Log.w("PostRepository", "Comments delete failed: ${e.message}")
            }

            // Postu sil
            postRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // Pagination ile postları getiren yeni fonksiyon
    suspend fun getPostsPaginated(lastVisiblePost: Post? = null): Result<Pair<List<Post>, Boolean>> {
        return try {
            Log.d("PostRepository", "Fetching paginated posts from Firestore...")
            
            var query = postsCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(pageSize.toLong())
            
            // Eğer son görünen post varsa, ondan sonraki postları getir
            if (lastVisiblePost != null) {
                query = query.startAfter(lastVisiblePost.createdAt)
            }
            
            val snapshot = query.get().await()
            val posts = snapshot.documents.mapNotNull { doc ->
                val post = doc.toObject(Post::class.java)
                post?.copy(id = doc.id)
            }
            
            // Daha fazla post olup olmadığını kontrol et
            val hasMore = posts.size >= pageSize
            
            Log.d("PostRepository", "Successfully fetched ${posts.size} posts, hasMore: $hasMore")
            Result.success(Pair(posts, hasMore))
        } catch (e: Exception) {
            Log.e("PostRepository", "Error fetching paginated posts", e)
            Result.failure(e)
        }
    }

    suspend fun likePost(postId: String, userId: String): Result<Unit> {
        return try {
            Log.d("PostRepository", "Updating like for post $postId by user $userId")
            val postRef = postsCollection.document(postId)
            val post = postRef.get().await().toObject(Post::class.java)
                ?: throw Exception("Post not found")

            val isLiked = post.likedBy.contains(userId)
            val newLikedBy = if (isLiked) {
                post.likedBy - userId
            } else {
                post.likedBy + userId
            }

            postRef.update(
                mapOf(
                    "likes" to (if (isLiked) post.likes - 1 else post.likes + 1),
                    "likedBy" to newLikedBy
                )
            ).await()
            Log.d("PostRepository", "Like updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PostRepository", "Error updating like", e)
            Result.failure(e)
        }
    }

    suspend fun addComment(postId: String, comment: Comment) {
        try {
            // Add comment to comments collection
            commentsCollection.document(comment.id).set(comment).await()

            // Update post's comments array
            postsCollection.document(postId).update(
                "comments", com.google.firebase.firestore.FieldValue.arrayUnion(comment)
            ).await()
        } catch (e: Exception) {
            throw Exception("Failed to add comment: ${e.message}")
        }
    }

    suspend fun getComments(postId: String): List<Comment> {
        return try {
            val snapshot = commentsCollection
                .whereEqualTo("postId", postId)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Comment::class.java)
            }
        } catch (e: Exception) {
            throw Exception("Failed to load comments: ${e.message}")
        }
    }

    suspend fun getUserPosts(userId: String): List<Post> {
        return try {
            Log.d("PostRepository", "Fetching posts for user $userId")
            val snapshot = postsCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val posts = snapshot.documents.mapNotNull { doc ->
                val post = doc.toObject(Post::class.java)
                post?.copy(id = doc.id)
            }
            Log.d("PostRepository", "Successfully fetched ${posts.size} posts for user")
            posts
        } catch (e: Exception) {
            Log.e("PostRepository", "Error fetching user posts", e)
            emptyList()
        }
    }

    suspend fun getUserPostsLimited(userId: String, limit: Int = 6): List<Post> {
        return try {
            Log.d("PostRepository", "Fetching limited posts ($limit) for user $userId")
            val snapshot = postsCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            val posts = snapshot.documents.mapNotNull { doc ->
                val post = doc.toObject(Post::class.java)
                post?.copy(id = doc.id)
            }
            Log.d("PostRepository", "Successfully fetched ${posts.size} limited posts for user")
            posts
        } catch (e: Exception) {
            Log.e("PostRepository", "Error fetching limited user posts", e)
            emptyList()
        }
    }
}