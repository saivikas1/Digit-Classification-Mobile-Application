import flask
import os
import pickle
import tensorflow as tf
import numpy as np
from PIL import Image
from tensorflow import keras
import sys
import cv2
import copy
import re 

server = flask.Flask(__name__)
@server.route('/', methods=['GET', 'POST'])

def handle_request():
    capturedImage = flask.request.files['image']
    predictedResult=str(predictImg(capturedImage))
    response=predictedResult
    tempStr = response[2:-2]
    tempStr = re.sub('\n','',tempStr)
    tempStr = re.sub('  ',' ',tempStr)
    l = list(tempStr.split(' '))
    if '' in l:
        l.remove('')
    floatList = [float(i) for i in l]
    max = float("-Inf")
    highProbVal = -1
    for i in range(0, len(l)):
        if floatList[i] > max:
            max = floatList[i]
            highProbVal = i
    
    # print(highProbVal)
    tempStr = str(highProbVal) + " " + str(max)
    print(tempStr)
    return tempStr

def predictImg(img):
    img = Image.open(img)
    img = np.asarray(img)
    img_gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    img_gray = cv2.blur(img_gray.copy(), (3,3))
    kernel_se = cv2.getStructuringElement(cv2.MORPH_RECT, (3,3))
    img_gray = cv2.dilate(img_gray.copy(), kernel_se, iterations = 1)
    img_gray = cv2.erode(img_gray.copy(), kernel_se, iterations = 1)
    img_gray = cv2.adaptiveThreshold(img_gray.copy(), 255, cv2.ADAPTIVE_THRESH_MEAN_C, cv2.THRESH_BINARY_INV, 51, 2)
    img_gray = np.expand_dims(img_gray, 2)
    img_gray=cv2.resize(img_gray, (14, 14))
    img_normalized = np.array(img_gray)/255
    img_normalized = np.array(img_normalized.flatten())
    #if np.sum(img_normalized) > np.sum(1 - img_normalized):
    if np.average(img_normalized) > 0.5:
        img_normalized = 1 - img_normalized
    model = keras.models.load_model("rmsprop_model.h5")
    prediction = model.predict(img_normalized.reshape(1,14,14,1))
    return prediction

server.run(host="0.0.0.0", port=5000, debug=True)
