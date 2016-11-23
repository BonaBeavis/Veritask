start-staging: package-staging
	mkdir -p ./../database
	docker-compose up --force-recreate

package-staging:
	git pull
	git checkout develop
	docker-compose run --no-deps --rm veritask activator clean stage

clean-staging:
	docker-compose stop
	docker-compose rm -f

rebuild-container:
	git pull
	git checkout develop
	docker-compose up --force-recreate --build

start-dev:
	docker-compose -f docker-compose-dev.yml up -d

clean-dev:
	docker-compose -f docker-compose-dev.yml stop
	docker-compose -f docker-compose-dev.yml rm -f
