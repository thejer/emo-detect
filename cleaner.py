import os
from glob import glob
from shutil import copy


def cleaner(counter):
    for root, dirs, files in os.walk('/path/to/Emotion_labels'):
        if files:
            with open("{}/{}".format(root, files[0])) as f:
                emotion_score = float(f.readline().strip())
                file_list = glob(
                    '/path/to/extended-cohn-kanade-images/cohn-kanade-images/{}/*'.format(
                        "/".join(root.split('/')[-2:])))
                file_list.sort()
                if emotion_score == 1:
                    copy_all(file_list, '/path/to/faces/anger/', counter)
                elif emotion_score == 5:
                    copy_all(file_list, '/path/to/faces/happiness/', counter)
                elif emotion_score == 6:
                    copy_all(file_list, '/path/to/faces/sadness/', counter)
            print("==================================")


def copy_all(from_dir, to, count):
    file_count = len(next(os.walk(to))[2])
    if file_count < 1000 and len(from_dir) >= count:
        for i in from_dir[-count:]:
            copy(i, to)
    neutral_path = '/path/to/faces/neutral/'
    neutral_count = len(next(os.walk(neutral_path))[2])
    if neutral_count < 1000:
        copy(from_dir[0], neutral_path)


def classifier():
    anger_count = len(next(os.walk("/path/to/faces/anger/"))[2])
    happiness_count = len(next(os.walk("/path/to/faces/happiness/"))[2])
    sadness_count = len(next(os.walk("/path/to/faces/sadness/"))[2])
    neutral_path = '/path/to/faces/neutral/'
    neutral_count = len(next(os.walk(neutral_path))[2])
    lens = [anger_count, sadness_count, happiness_count, neutral_count]
    min_len = min(lens)
    print(min_len)
    for i in lens:
        print(i)
    counter = 1

    while min_len < 1000:
        print(min_len)
        cleaner(counter)
        anger_count = len(next(os.walk("/path/to/faces/anger/"))[2])
        happiness_count = len(next(os.walk("/path/to/faces/happiness/"))[2])
        sadness_count = len(next(os.walk("/path/to/faces/sadness/"))[2])
        neutral_path = '/path/to/faces/neutral/'
        neutral_count = len(next(os.walk(neutral_path))[2])
        lens = [anger_count, sadness_count, happiness_count, neutral_count]
        min_len = min(lens)
        counter += 1


classifier()
