import "source-map-support/register";
import { App } from "aws-cdk-lib";
import { Housekeeper } from "../lib/housekeeper";

const app = new App();
new Housekeeper(app, "Housekeeper-PROD", { stack: "ophan", stage: "PROD" });
