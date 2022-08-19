import csv
import os
import pandas as pd
import matplotlib.pyplot as plt
def editCsvFile(inputFileName, outputFileName):
    index = inputFileName.find("imu")
    if index == -1:
        ## Then it is vio file
        data = ['timeStamp', " ","positionx", "positiony", "positionz", "quaternionx", "quaterniony", "quaternionz", "quaternionw"]
    else:
        ## It is imu file
        data = ['timeStamp', " ", " ", " ", "gyroX", "gyroY", "gyroZ", " ", " ", " ", "accelX", "accelY", "accelZ"]

    with open(outputFileName + ".csv", 'w') as file:
        writer = csv.writer(file)
        #writer.writerow(data)
        with open(inputFileName + ".csv") as file_obj:
            if index == -1:
            # Create reader object by passing the file 
            # object to reader method
                reader_obj = csv.reader(file_obj)
                
                # Iterate over each row in the csv 
                # file using reader object
                deneme = []; 
                counter = 0
                for row in reader_obj:
                    if counter == 0:
                        writer.writerow(data)
                        row[0] = row[0][1:]
                        writer.writerow(row)
                    else:
                        deneme = row[1:]
                        for string in row:
                            string.replace(" ","")
                            
                        writer.writerow(deneme)
                    counter = counter + 1
            else:
                reader_obj = csv.reader(file_obj)
                
                # Iterate over each row in the csv 
                # file using reader object
                counter = 0
                for row in reader_obj:
                    if counter == 0:
                        writer.writerow(data)
                    writer.writerow(row)
                    counter = counter + 1
            

def plotTrajectory(vioFileName):
    """
    dfSynched = pd.read_csv(vioFileName + 'Editted.csv')
    xEditted = dfSynched['positionx']
    zEditted = dfSynched['positionz']

    plt.plot(xEditted, zEditted, label = 'Synched Data', linewidth = '1')

    plt.show()"""
    df = pd.read_csv(vioFileName + 'Editted.csv')
    fig = plt.figure()
    ax = plt.axes(projection='3d')
    positionx = df['positionx']
    positiony = df['positiony']
    positionz = df['positionz']
    ax.plot3D(positionx, positionz, positiony, 'gray')
    ax.set_title('Estimated 3DoF Positions')
    ax.set_xlabel('X')
    ax.set_ylabel('Z')
    ax.set_zlabel('Y')
    plt.show()

def plotImuValues(imuFileName):
    df = pd.read_csv(imuFileName + 'Editted.csv')
    time = df['timeStamp']
    data = df['accelZ']
    plt.plot(time, data)
    plt.xlabel('Time')
    plt.ylabel('Z (in g)')
    plt.show()

if __name__ == "__main__":

    vioFileName = 'User3WalkingHandvio1'
    imuFileName = 'User3WalkingHandimu1'

    isFile = os.path.isfile(vioFileName + 'Editted.csv')
    if not isFile:
        editCsvFile(vioFileName, vioFileName+'Editted')
        editCsvFile(imuFileName, imuFileName+'Editted')
    else:
        plotTrajectory(vioFileName)
        plotImuValues(imuFileName)

    
