
#
library(igraph)

netName = "~/Development/SNET_data/hires_accordeon"
netName = "~/Development/SNET_data/2017_07_27_23_20"

#parameters
fileName = paste(netName,"net_parameters.csv",sep="/")
print(fileName)
param = read.csv(fileName)

color = "black"
plot(param$iteration,param$neurons,type="l",col=color,xlab = "Timestep", ylab = "Number of neurons", main=netName
     #,xlim= c(0,50),ylim= c(9300,9400)
     )
plot(param$iteration,param$connections/1000,type="l",col=color, xlab = "Timestep", ylab = "Connections/1000",
     main=netName)

points(param$iteration,param$neurons,type="l",col="red")
points(param$iteration,param$connections/1000,type="l",col="red")

plot_neurons(param1)
legend(2000,1200, c("With noise","No noise"),
       lty = 1, bty = "n",
       lwd=c(2.5,2.5),col=c("black","red")) 

plot_weights(param1)
legend(1200,130,  c("With noise","No noise"),
       lty = 1, bty = "n",
       lwd=c(2.5,2.5),col=c("black","red")) 

plot_neurons = function(parameters){
  plot(parameters$iteration,parameters$neurons,type="l",col="red",xlab = "Timestep", ylab = "Number of neurons",
       xlim= c(0,3000), ylim= c(0,1500), cex.lab=1.5, cex.axis=1.5)
}

plot_weights = function(parameters){
  plot(parameters$iteration,parameters$connections/1000,type="l",col="red",xlab = "Timestep", ylab = "Number of weights / 1,000",
       xlim= c(0,3000), ylim= c(0,200), cex.lab=1.5, cex.axis=1.5)
}

#performance
netName = "../oswald_bike"
fileName = paste(netName,"performance.csv",sep="/")
print(fileName)
perf = read.csv(fileName)

color = "black"
plot(perf$iteration,perf$error,type="l",xlab = "Timestep", ylab = "Error", col=color,
     main=netName, xlim = c(0,800), ylim = c(0,1))
plot(perf$iteration,perf$surprise,type="l",xlab = "Timestep", ylab = "Surprise", col=color,
     main=netName, xlim = c(0,800), ylim = c(0,1))
plot(perf$iteration,perf$illusion,type="l",xlab = "Timestep", ylab = "Illusion", col=color,
     main=netName, xlim = c(0,800), ylim = c(0,1))

plot_surprise(perf)

points(perf$iteration,perf$error,type="l", col="red")
points(perf$iteration,perf$surprise,type="l", col="red")
points(perf$iteration,perf$illusion,type="l", col="red")
legend(350,400, c("Simple video","Complex video"),lty = 1, bty = "n",lwd=c(2.5,2.5),col=c("blue","red")) 
abline(v=40)


#memories
netName = "~/Development/SNET_data/2017_07_05_20_35"
fileName = paste(netName,"memories.csv",sep="/")#reminicsing_
fileName = paste(netName,"reminiscing_memories.csv",sep="/")#reminicsing_
print(fileName)
memories = read.csv(fileName)

per_iteration = c()
l = memories$iteration[length(memories$iteration)]
for(i in 0:l){
  per_iteration = c(per_iteration, sum(memories$iteration == i))
}
plot(per_iteration, type="l", xlab = "Timestep", ylab = "Memory Size", main=fileName
     , xlim = c(0,400)
     )
points(per_iteration, type="l", col="red")

#neurons in 3d
library(rgl)
netName = "../2017_07_02_22_52"
step = "454"
fileName = paste(netName,step,"positions.csv",sep="/")
print(fileName)
positions = read.csv(fileName)
#sy
plot3d(x = positions$x , y = positions$y , z = positions$sx, pch=21)
browseURL(paste("file://", writeWebGL(dir=file.path(netName, "webGL"), width=500), sep=""))




plot_surprise = function(parameters){
  plot(perf$iteration,perf$surprise,type="l",xlab = "Timestep", ylab = "Surprise", col="red",
       xlim= c(0,400), ylim= c(0,300), cex.lab=1.5, cex.axis=1.5)
}

plot_neurons(param)
plot_weights(param)

img_path = paste(netName,"images","neurons.pdf", sep="/")
pdf(file = img_path, width = 4, height = 5, family = "Helvetica")
plot_neurons(param)
dev.off()

img_path = paste(netName,"images","weights.pdf", sep="/")
pdf(file = img_path, width = 4, height = 5, family = "Helvetica")
plot_weights(param)
dev.off()

#debug
netName = "~/Development/SNET_data/2017_07_03_23_58"
fileName = paste(netName,"net.csv",sep="/")
net =  read.csv(fileName)


#weights
netName = "~/Development/SNET_data/2017_07_03_21_54"

fileName = paste(netName,"weights_50.csv",sep="/")
plotnet()

img_path = paste(netName,"images","net.pdf", sep="/")

pdf(file = img_path, width = 5.2, height = 4.3, family = "Helvetica")
plotnet()
dev.off()

plotnet = function(){
  print(fileName)
  w = read.csv(fileName)
  
  ur = unique(w$from)
  uc = unique(w$to)
  total = unique(c(ur,uc))
  nr = length(total)
  nc = length(total)
  m = matrix(0,nr,nc)
  rownames(m) = sort(total)
  colnames(m) = sort(total)
  for(i in 1:length(w$from)){
    m[as.character(w$from[i]),as.character(w$to[i])] = w$weight[i]
  }
  
  net=graph.adjacency(m,mode="directed",weighted=TRUE)#,diag=FALSE)
  plot.igraph(net,vertex.size=25, vertex.label.color="black",#layout=layout.circle,#vertex.label=nodeNames,
              edge.color="black", vertex.color="grey", vertex.frame.color="grey", edge.width=E(net)$weight, edge.arrow.size=0.25,edge.curved=TRUE)
}

#bweights
fileName = paste(netName,"bweights_98.csv",sep="/")
print(fileName)
w = read.csv(fileName)

s = subset(w, w$to == 23)
print(s)

for(i in 1:length(s1$weight)){
  sub = subset(s2, s2$to == s1$to[i])
  if(length(sub$from)==0){
    print(s1$to[i])
  }else if((sub$weight-s1$weight[i])!=0){
    print(s1$weight[i])
    print(sub)
  }
}

#PCA
#
install.packages("ggbiplot")
library(pixmap)
library(data.table)

r = 15
c = 50*50
df= data.frame(matrix(, nrow=r, ncol=c))

j=0
for(i in 25:39){
  iname = sprintf("%02d.ppm", i);
  filepath = paste("/Users/lana/Desktop/prgm/SNet/images/Oswald/bike/small/ppm/",iname, sep = "")
  img = read.pnm(filepath)
  bw = img@red
  v = unlist(bw)
  j=j+1
  df[j,] = v
}
#pca
pca = prcomp(df, center = TRUE, scale. = TRUE) 
plot(pca, type = "l")
plot(pca$x)
text(pca$x[,1]+5, pca$x[,2], c(1:23), col="blue")

points(pca$x, col="black")



