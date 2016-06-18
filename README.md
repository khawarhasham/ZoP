# ZoP App
=========

This document provides a brief background of the ZoP app and also lists the features that have been added in it. 

## Background
The idea of ZoP app came out as an extension of the Learning Layers project. In this project, a beautiful app, named [AchSo!][layers] , has been developed that helps users to capture, annotate and share videos among their peers. This way it helps the users to provide video content and also to collaborate over it. The ZoP app is based on the AchSo! app and extends it further by adding social interactive features such as discussion/comments, inline replies, likes/ratings etc. These features have been implemented using the [Social Semantic Server (SSS)][SSS] REST API.

## ZoP Features
Following are the list of features that have been added in the ZoP app. The implementation of these features make use of the SSS REST API as discussed below.

###1. Discussion/comments
**Purpose:** The rationale for having such a feature is to enable social interaction between users on a video. By having such feature, users can discuss various aspects of the video or learning process and can pose different questions related to particular concepts presented in a video.

**Implementation:** Using SSS RESTful API (version V3) endpoint /discs. By default, the created discussion is treated as private in SSS. In order to allow multiple users to post comments in a discussion, this has to be made public. This can be achieved by using the endpoint /entities/{entityid}/share, where entityid is the discussion id in SSS, and provide a parameter setPublic=True.

###2. Comment Reply
**Purpose:** The main focus of this feature is to enable users to reply to a comment that was posted in a discussion. This would make the application more socially interactive and also helps in keeping separate threads within main discussion. As of now, the replies to comments will be treated all at the same level i.e. single level.

**Implementation:** First, the discussion will be created in the SSS with api call /discs. When a user posts a comment, it will be added in SSS. The SSS service assigns a new ID to each new entry in the /discs. Using the ID assigned to each entry or comment, we can call /entities/{entity}/comments to add comment reply.

###3. Likes/Ratings
**Purpose:** The purpose of this feature is to enable users to like/rate a video or comment in discussion. The comments can be in the form of a question as well. This feature will help the system to calculate things such as ‘Most Popular’ comment/question. With such an interface, it will help in identifying the popular questions that users are interested in. 

**Implementation:** Using SSS RESTful API (version V3) endpoint /likes and /ratings. The current implementation uses /likes endpoint to like a comment. The comment likes can later be retrieved by setting 'setLikes=True' while calling /discs/filtered/targets/{vidoeid} or /discs/filtered/{discid}. The returned result contains the *likes* object, which includes *likes, dislikes and like* properties. The *likes* property represents the total number of likes this entity has and *dislikes* represents the total number of dislikes. The *like* property means whether the requesting user has already likes this entity or not. Based on number of likes, we can sort the returned result and present to the user as 'Most Popular' comments.

###4. User Interests
**Purpose:** In the existing Learning Layer tools or services, such as achrails and AchSo app, a user can not provide his interests and then search other users based on the common interests. During our discussions with the potentials users from Cinefest, a feature to add and list ones interests was discussed. This information can later be used for searching the users of similar interests. This will aid in creating more like-minded user social circles.

**Implementation:** In the current ZoP app, a facility has been added that shows the user profile along with the mechanism to add his/her interests. These interests are stored using the /tags endpoint of SSS REST API. 

[layers]: http://developer.learning-layers.eu/tools/ach-so/
[SSS]: https://github.com/learning-layers/SocialSemanticServer/