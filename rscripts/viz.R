
#
library(igraph)

netName = "../Ecal_oswald_accordeon"
netName = "../Ecal_oswald_bike"

#parameters
fileName = paste(netName,"net_parameters.csv",sep="/")
print(fileName)
param = read.csv(fileName)

plot(param$iteration,param$neurons,type="l",col="red",xlab = "Timestep", ylab = "Number of neurons", main=netName
     #,xlim= c(0,1000), ylim= c(0,1800)
     )
plot(param$iteration,param$connections,type="l",col="red", xlab = "iteration", ylab = "connections",
     main=netName)

points(param$iteration,param$neurons,type="l",col="blue")
points(param$iteration,param$connections,type="l",col="blue")

plot_neurons(param1)
legend(400,2000, c("Simple video","Complex video"),
       lty = 1,
       lwd=c(2.5,2.5),col=c("blue","red")) 

plot_weights(param)

#performance
fileName = paste(netName,"performance.csv",sep="/")
print(fileName)
perf = read.csv(fileName)
plot(perf$iteration,perf$surprise,type="l",xlab = "iteration", ylab = "surprise", col="red", main=netName, xlim= c(0,200))

points(perf$iteration,perf$surprise,type="l", col="blue")

plot_neurons = function(parameters){
  plot(parameters$iteration,parameters$neurons,type="l",col="red",xlab = "Timestep", ylab = "Number of neurons",
       xlim= c(0,800), ylim= c(0,2010), cex.lab=1.5, cex.axis=1.5)
}

plot_weights = function(parameters){
  plot(parameters$iteration,parameters$connections/1000,type="l",col="red",xlab = "Timestep", ylab = "Number of weights x 1,000",
       xlim= c(0,800), ylim= c(0,400), cex.lab=1.5, cex.axis=1.5)
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
fileName = paste(netName,"weights_200.csv",sep="/")
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
