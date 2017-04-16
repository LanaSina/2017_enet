
#
library(igraph)

netName = "../ECAL_kitti"
netName = "../ECAL_oswald_bike"

#parameters
fileName = paste(netName,"net_parameters.csv",sep="/")
print(fileName)
param = read.csv(fileName)

plot(param$iteration,param$neurons,type="l",col="red",xlab = "Timestep", ylab = "Number of neurons", main=netName
     #,xlim= c(0,1000), ylim= c(0,1800)
     )
plot(param$iteration,param$connections/1000,type="l",col="red", xlab = "iteration", ylab = "connections",
     main=netName)

points(param$iteration,param$neurons,type="l",col="purple")
points(param$iteration,param$connections/1000,type="l",col="purple")

plot_neurons(param1)
legend(350,2300, c("No noise","With noise"),
       lty = 1, bty = "n",
       lwd=c(2.5,2.5),col=c("blue","red")) 

plot_weights(param3)
legend(20,300,  c("No noise","With noise"),
       lty = 1, bty = "n",
       lwd=c(2.5,2.5),col=c("blue","red")) 


#performance
fileName = paste(netName,"performance.csv",sep="/")
print(fileName)
perf = read.csv(fileName)
plot(perf$iteration,perf$surprise,type="l",xlab = "iteration", ylab = "surprise", col="red", main=netName
     , xlim= c(0,100)
     )

i=1
d = c()
while((i+10)< length(perf$surprise)){
  d = c(d,mean(perf$surprise[i:(i+10)]))
  i=i+10
}

plot(d, type="l",  xlim= c(0,40))
points(d, type="l", col="red")

plot_surprise(perf)
points(perf$iteration,perf$surprise,type="l", col="blue")
legend(350,400, c("Simple video","Complex video"),lty = 1, bty = "n",lwd=c(2.5,2.5),col=c("blue","red")) 
abline(v=40)

plot_neurons = function(parameters){
  plot(parameters$iteration,parameters$neurons,type="l",col="red",xlab = "Timestep", ylab = "Number of neurons",
       xlim= c(0,800), ylim= c(0,2310), cex.lab=1.5, cex.axis=1.5)
}

plot_weights = function(parameters){
  plot(parameters$iteration,parameters$connections/1000,type="l",col="red",xlab = "Timestep", ylab = "Number of weights x 1,000",
       xlim= c(0,800), ylim= c(0,300), cex.lab=1.5, cex.axis=1.5)
}

plot_surprise = function(parameters){
  plot(perf$iteration,perf$surprise,type="l",xlab = "Timestep", ylab = "Surprise", col="red",
       xlim= c(0,100), ylim= c(0,300), cex.lab=1.5, cex.axis=1.5)
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

#weights
fileName = paste(netName,"weights_800.csv",sep="/")
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

r = 40
c = 50*50
df= data.frame(matrix(, nrow=r, ncol=c))

for(i in 0:39){
  iname = sprintf("%02d.ppm", i);
  filepath = paste("/Users/lana/Desktop/prgm/SNet/images/Oswald/bike/small/ppm/",iname, sep = "")
  img = read.pnm(filepath)
  bw = img@red
  v = unlist(bw)
  df[(i+1),] = v
}
#pca
pca = prcomp(df, center = TRUE, scale. = TRUE) 
plot(pca, type = "l")
plot(pca$x)
text(pca$x[,1]+5, pca$x[,2], c(1:40))

points(pca$x, col="blue")



