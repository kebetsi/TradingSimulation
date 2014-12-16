import csv, random
import nltk
import re
import pickle
import sys
from collections import Counter

class SentimentUtils:
	
	@staticmethod
	def getFoldPartition(dataset, kfold):
		for k in xrange(kfold):
			training = [x for i, x in enumerate(dataset) if i % kfold != k]
			validation = [x for i, x in enumerate(dataset) if i % kfold == k]
			yield training, validation
		
	@staticmethod		
	def getStopWordList(filePath):
		stopWords = []
		fp = open(filePath, 'r')
		line = fp.readline()
		while line:
			word = line.strip()
			stopWords.append(word)
			line = fp.readline()
		fp.close()
		return stopWords
	

class TwitterSentimentClassifier:
	
	def __init__(self,load=False,
						classifierPath="classifier.pickle", 
						fListPath="fList.pickle"):

		if load:
			f = open(fListPath)
			self.featureList = pickle.load(f)
			f.close()
			f = open(classifierPath)
			self.classifier = pickle.load(f)
			f.close()
		else:
			self.featureList = []
			self.classifier = None

	def get_feature_vec(self,tweets,stopWords):
		featureVector = []
		words = tweets.split()
		for w in words:
			if(w in stopWords or len(w) <= 3):
				continue
			else:
				featureVector.append(w.lower())
		return featureVector

	
	def clean_tweet(self, tweet):
		# Convert the text to lower case
		post = tweet.lower()
		# Remove all urls
		post = re.sub('(http(s?)://)([a-zA-Z0-9\/\.])*', ' ', post)
		# Remove everything that is not a word
		post = re.sub('[^(\w&\s)]|\(|\)|\d', ' ', post)
		return post
		
	def extract_features(self,tweet):
		tweet_words = set(tweet)
		features = {}
		for word in self.featureList:
			features['contains(%s)' % word] = (word in tweet_words)
		return features
		
	def classify_tweet(self, tweet):
		processedTweet = self.clean_tweet(tweet)
		stopWords = SentimentUtils.getStopWordList('stopwords.txt')
		featureVec = self.get_feature_vec(processedTweet, stopWords)
		return self.classifier.classify(self.extract_features(featureVec))
		
	def train(self, filePath, classificationModel, crossValidation=False):
		tweetsRaw = list(csv.reader(open(filePath, 'rb'), delimiter=',', quotechar='"'))
		random.shuffle(tweetsRaw)
		stopWords = SentimentUtils.getStopWordList('stopwords.txt')
		balanceSentiment = {'positive':0,'negative':0,'neutral':0}
		
		tweets = []
		for row in tweetsRaw:
			sentiment = row[1]
			if sentiment != 'positive' and sentiment != 'negative' and sentiment != 'neutral':
				continue
			#Want equal proportion of neg/pos/neutral tweets
			#Since we have only 691 neg tweets, it is the upperband
			if balanceSentiment[sentiment] >= 691:
				continue
			balanceSentiment[sentiment] += 1
			tweet = row[0]
			processedTweet = self.clean_tweet(tweet)
			featureVector = self.get_feature_vec(processedTweet, stopWords)
			self.featureList.extend(featureVector)
			tweets.append((featureVector, sentiment));
		
		wordOccurences = Counter(self.featureList).most_common(1000)
		self.featureList = zip(*wordOccurences)[0]
		fvecs = nltk.classify.util.apply_features(self.extract_features, tweets)

		if not crossValidation:
			ratioTrainTest = 0.8
			lenTrain = int(ratioTrainTest*len(fvecs))
			train_set = fvecs[0:lenTrain]
			test_set = fvecs[lenTrain:]
			
			self.classifier = classificationModel.train(train_set);
			print 'Training complete'
			accuracy = nltk.classify.accuracy(self.classifier, test_set)
			print 'Accuracy : \t%.2f%%' % round(accuracy*100.0,2)
			
			f = open('classifier.pickle', 'wb')
			pickle.dump(self.classifier, f)
			f.close()
			f = open('fList.pickle', 'wb')
			pickle.dump(self.featureList, f)
			f.close()
		else:
			kfold = 5
			current = 0
			print '########################'
			print '%d-fold crossvalidation' % kfold
			print '########################'
			print 'Iteration\tAccuracy'
			for (train_set, test_set) in SentimentUtils.getFoldPartition(fvecs, kfold):
				current += 1
				self.classifier = classificationModel.train(train_set);
				accuracy = nltk.classify.accuracy(self.classifier, test_set)
				print '%d\t\t%.2f%%' % (current, round(accuracy*100.0,2))

if __name__=='__main__':
	classificationModel = nltk.NaiveBayesClassifier
	sentimentClassifier = TwitterSentimentClassifier(load=True)
	print sentimentClassifier.classify_tweet(sys.argv[1])
	#sentimentClassifier.train('mergedWithApple.csv', classificationModel, crossValidation=False)
