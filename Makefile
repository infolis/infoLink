DOCKER = docker
IMGNAME = 'infolis/infolink'
APPNAME = 'infolink-app'

.PHONY: build

build:
	http_proxy=localhost:8123 $(DOCKER) build -t $(IMGNAME) .

stop:
	$(DOCKER) stop $(APPNAME)

delete:
	$(DOCKER) rm $(APPNAME)

run:
	$(MAKE) -s stop || echo "Container not running"
	$(MAKE) -s delete || echo "Container does not exist"
	@$(DOCKER) run -d \
		-p 8080:8080 \
		-v $(PWD)/docker/infolis-config.properties:/etc/infolis-config.properties \
		-v $(PWD)/docker/infolis-files:/infolis-files \
		--name $(APPNAME) \
		$(IMGNAME)

bash:
	$(DOCKER) exec -it $(APPNAME) bash -i
